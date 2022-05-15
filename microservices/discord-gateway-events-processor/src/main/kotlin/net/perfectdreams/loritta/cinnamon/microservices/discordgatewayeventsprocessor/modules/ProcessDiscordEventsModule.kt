package net.perfectdreams.loritta.cinnamon.microservices.discordgatewayeventsprocessor.modules

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import dev.kord.gateway.Event
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import net.perfectdreams.loritta.cinnamon.microservices.discordgatewayeventsprocessor.DiscordGatewayEventsProcessor
import net.perfectdreams.loritta.cinnamon.microservices.discordgatewayeventsprocessor.utils.KordDiscordEventUtils
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

abstract class ProcessDiscordEventsModule(private val rabbitMQQueue: String) {
    companion object {
        private val logger = KotlinLogging.logger {}

        const val MAX_ACTIVE_EVENTS_THRESHOLD = 16
    }

    val clazzName = this::class.simpleName
    val activeEvents = ConcurrentLinkedQueue<Job>()
    var launchedEvents = 0
    var consumerRestarts = 0
    val mutex = Mutex()
    var moduleConsumerTag: String? = null
    var lastMessageReceivedAt: Instant? = null

    abstract suspend fun processEvent(event: Event)

    /**
     * Setups the required queue binds for this module on the [channel]
     */
    abstract fun setupQueueBinds(channel: Channel)

    /**
     * Setups a RabbitMQ consumer for this module on the [channel]
     */
    fun setupConsumer(channel: Channel) {
        val args = mapOf(
            "x-max-length" to 10000 // Max 10k messages per queue
        )

        channel.queueDeclare(rabbitMQQueue, true, false, false, args)
        channel.basicQos(MAX_ACTIVE_EVENTS_THRESHOLD, false) // MAX_ACTIVE_EVENTS_THRESHOLD prefetch per channel, to avoid overloading the service
        setupQueueBinds(channel)

        moduleConsumerTag = startConsumingMessages(channel)
    }

    private fun startConsumingMessages(channel: Channel): String {
        consumerRestarts++
        logger.info { "Starting consumer for $clazzName, the consumer has been started $consumerRestarts times!" }
        val consumerTag = channel.basicConsume(
            rabbitMQQueue,
            false,
            DeliverCallback { consumerTag, message ->
                val discordEvent = KordDiscordEventUtils.parseEventFromJsonString(message.body.toString(Charsets.UTF_8))
                if (discordEvent != null) {
                    launchedEvents++
                    lastMessageReceivedAt = Instant.now()

                    val coroutineName = "Event ${discordEvent::class.simpleName} for $clazzName"
                    launchEventJob(channel, coroutineName) {
                        try {
                            processEvent(discordEvent)
                        } catch (e: Throwable) {
                            logger.warn(e) { "Something went wrong while trying to process $coroutineName! We are going to ignore it and ack the message, to avoid an acknowledgement timeout..." }
                        }

                        channel.basicAck(message.envelope.deliveryTag, false)
                    }
                } else {
                    logger.warn { "Unknown Discord event received! We are going to ack the event just so it goes away... kthxbye!" }
                    channel.basicAck(message.envelope.deliveryTag, false)
                }
            },
            CancelCallback {
                logger.warn { "Consumer has been cancelled unexpectedly! $it" }
            }
        )
        logger.info { "Successfully created consumer for $clazzName! The consumer tag is $consumerTag" }
        return consumerTag
    }

    /**
     * Helper method to bind a queue using [Channel.queueBind], providing [rabbitMQQueue] as the queue and [DiscordGatewayEventsProcessor.RABBITMQ_EXCHANGE_NAME] as the exchange name
     *
     * @see Channel.queueBind
     */
    fun Channel.queueBindToModuleQueue(routingKey: String): AMQP.Queue.BindOk = this.queueBind(rabbitMQQueue, DiscordGatewayEventsProcessor.RABBITMQ_EXCHANGE_NAME, routingKey)

    fun launchEventJob(channel: Channel, coroutineName: String, block: suspend CoroutineScope.() -> Unit) {
        val start = System.currentTimeMillis()
        val job = GlobalScope.launch(
            CoroutineName(coroutineName),
            block = block
        )

        // We need to launch because we need to lock to avoid registering two channel consumers
        GlobalScope.launch {
            mutex.withLock {
                activeEvents.add(job)
                if (moduleConsumerTag != null && activeEvents.size >= MAX_ACTIVE_EVENTS_THRESHOLD) {
                    // Too many events, let's cancel our consumer
                    logger.warn { "There are more than $MAX_ACTIVE_EVENTS_THRESHOLD active events in $clazzName! We will cancel our consumer and resume it after we get our active events below the $MAX_ACTIVE_EVENTS_THRESHOLD threshold..." }
                    channel.basicCancel(moduleConsumerTag)
                    moduleConsumerTag = null
                }

                // Yes, the order matters, since sometimes the invokeOnCompletion would be invoked before the job was
                // added to the list, causing leaks.
                // invokeOnCompletion is also invoked even if the job was already completed at that point, so no worries!
                job.invokeOnCompletion {
                    activeEvents.remove(job)

                    val diff = System.currentTimeMillis() - start
                    if (diff >= 60_000) {
                        logger.warn { "Coroutine $job took too long to process! ${diff}ms" }
                    }

                    // Because we will execute in another coroutine, there *shouldn't* be a deadlock
                    GlobalScope.launch {
                        mutex.withLock {
                            val activeEvents = activeEvents.size
                            if (moduleConsumerTag == null && MAX_ACTIVE_EVENTS_THRESHOLD > activeEvents) {
                                logger.info { "There are less than $MAX_ACTIVE_EVENTS_THRESHOLD active events (active events: $activeEvents) in $clazzName! We will restart the consumer..." }
                                moduleConsumerTag = startConsumingMessages(channel)
                            }
                        }
                    }
                }
            }
        }
    }
}