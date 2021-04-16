package net.perfectdreams.loritta.platform.cli.entities

import net.perfectdreams.loritta.common.entities.LorittaEmbed
import net.perfectdreams.loritta.common.entities.LorittaMessage
import net.perfectdreams.loritta.common.entities.MessageChannel
import java.io.File

class CLIMessageChannel : MessageChannel {
    override suspend fun sendMessage(message: LorittaMessage) {
        println(message.content)
        message.embed?.let { println(translateEmbed(it)) }

        message.files.entries.forEach {
            File(it.key).writeBytes(it.value)
            println("Saved file ${it.key}")
        }
    }

    private fun translateEmbed(embed: LorittaEmbed): String = buildString {
        append("Title: ${embed.title}\n")
        append("Description: ${embed.description}\n")

        embed.fields.forEach {
            append("${it.name}: ${it.value}\n")
        }
    }
}