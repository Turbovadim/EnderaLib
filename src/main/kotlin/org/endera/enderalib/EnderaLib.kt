package org.endera.enderalib

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.bstats.MetricsLite
import org.endera.enderalib.config.ConfigScheme
import org.endera.enderalib.config.defaultConfig
import org.endera.enderalib.utils.configuration.PluginException
import org.endera.enderalib.utils.configuration.configLoadCreationHandler
import org.endera.enderalib.utils.isFolia
import java.io.File

internal lateinit var configFile: File
internal lateinit var config: ConfigScheme
var isFolia: Boolean = false

internal class EnderaLib : JavaPlugin() {

    override fun onEnable() {

        isFolia = isFolia()
        val metrics = MetricsLite(this, 23669)
        configFile = File("${dataFolder}/config.yml")

        getCommand("enderalib")?.setExecutor(EnderaLibCommand(this))

        try {
            val loadedConfig = configLoadCreationHandler(
                configFile = configFile,
                dataFolder = dataFolder,
                defaultConfig = defaultConfig,
                logger = logger,
                serializer = ConfigScheme.serializer()
            )
            org.endera.enderalib.config = loadedConfig
        } catch (e: PluginException) {
            logger.severe("Critical error loading configuration: ${e.message}")
            server.pluginManager.disablePlugin(this)
        }
        this.logger.info("Plugin is loaded")
    }

    inner class EnderaLibCommand(private val plugin: EnderaLib) : CommandExecutor {
        override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
            if (cmd.name.equals("enderalib", ignoreCase = true)) {
                if (args.isNotEmpty() && args[0].equals("version", ignoreCase = true)) {
                    sender.sendMessage("§aEnderaLib §eversion: §7${plugin.description.version}")
                    return true
                } else {
                    sender.sendMessage("§cThis command doesn't exist")
                }
            }
            return false
        }
    }
}