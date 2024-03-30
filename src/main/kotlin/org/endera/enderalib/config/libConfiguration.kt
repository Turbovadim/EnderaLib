package org.endera.enderalib.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigScheme(
    val messages: MessagesSchema,
)

@Serializable
data class MessagesSchema(
    val prefix: String,
    val noPermission: String,
)

val defaultConfig = ConfigScheme(
    messages = MessagesSchema(
        prefix = "<gradient:#5e4fa2:#f79459>[EnderaLib]</gradient>",
        noPermission = "{prefix} You don't have permission to do this."
    )
)