package org.endera.enderalib.config

import kotlinx.serialization.Serializable
import org.endera.enderalib.utils.configuration.Comment

@Serializable
internal data class ConfigScheme(
    @Comment("""
        This message will be thrown when Player.checkPermission() is used
        This message will be thrown when Player.checkPermission() is used2
        This message will be thrown when Player.checkPermission() is used3
    """)
    val messages: MessagesSchema = MessagesSchema(),
    @Comment("This message will be thrown when Player.checkPermission() is used 228")
    val sexSexer: String = "sosal?",
)

@Serializable
internal data class MessagesSchema(
    val prefix: String = "<gradient:#5e4fa2:#f79459>[EnderaLib]</gradient>",
    @Comment("This message will be thrown when Player.checkPermission() is used")
    val noPermission: String = "{prefix} You don't have permission to do this.",
)

//internal val defaultConfig = ConfigScheme(
//    messages = MessagesSchema(
//        prefix = "<gradient:#5e4fa2:#f79459>[EnderaLib]</gradient>",
//        noPermission = "{prefix} You don't have permission to do this."
//    ),
//    sexSexer = "sdfsdffsddsf"
//)