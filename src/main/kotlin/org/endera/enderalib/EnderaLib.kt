package org.endera.enderalib

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class EnderaLib : JavaPlugin() {

    override fun onEnable() {
        this.logger.info("Plugin is loaded")
        getCommand("enderalib")?.setExecutor(EnderaLibCommand(this))
    }

    inner class EnderaLibCommand(private val plugin: EnderaLib) : CommandExecutor {
        override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
            if (cmd.name.equals("enderalib", ignoreCase = true)) {
                if (args.isNotEmpty() && args[0].equals("version", ignoreCase = true)) {
                    sender.sendMessage("§aEnderaLib §eversion: §7${plugin.description.version}")
                    return true
                } else {
                    sender.sendMessage("§cТакой команды не существует")
                }
            }
            return false
        }
    }
}
