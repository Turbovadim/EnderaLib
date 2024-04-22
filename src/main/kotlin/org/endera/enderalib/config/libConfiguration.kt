package org.endera.enderalib.config

import kotlinx.serialization.Serializable

@Serializable
internal data class ConfigScheme(
    val messages: MessagesSchema,
)

@Serializable
internal data class MessagesSchema(
    val prefix: String,
    val noPermission: String,
)

internal val defaultConfig = ConfigScheme(
    messages = MessagesSchema(
        prefix = "<gradient:#5e4fa2:#f79459>[EnderaLib]</gradient>",
        noPermission = "{prefix} You don't have permission to do this."
    )
)