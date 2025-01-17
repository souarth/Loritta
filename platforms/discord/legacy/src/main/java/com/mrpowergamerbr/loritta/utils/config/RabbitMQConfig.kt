package com.mrpowergamerbr.loritta.utils.config

import kotlinx.serialization.Serializable

@Serializable
data class RabbitMQConfig(
    val host: String,
    val virtualHost: String,
    val username: String,
    val password: String
)