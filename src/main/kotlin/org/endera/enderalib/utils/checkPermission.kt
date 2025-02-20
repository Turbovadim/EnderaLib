package org.endera.enderalib.utils

import org.bukkit.command.CommandSender
import org.endera.enderalib.adventure.stringToComponent
import org.endera.enderalib.config

@Suppress("unused")
fun CommandSender.checkPermission(
    permission: String,
    executable: () -> Unit
) {
    if (hasPermission(permission)) {
        executable()
    } else {
        sendMessage(config.messages.noPermission.replace("{prefix}", config.messages.prefix).stringToComponent())
    }

}