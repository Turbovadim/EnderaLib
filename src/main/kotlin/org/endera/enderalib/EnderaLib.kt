package org.endera.enderalib

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.adventure.stringToComponent
import org.endera.enderalib.bstats.MetricsLite
import org.endera.enderalib.config.ConfigScheme
import org.endera.enderalib.config.defaultConfig
import org.endera.enderalib.utils.PluginException
import org.endera.enderalib.utils.configuration.ConfigurationManager
import org.endera.enderalib.utils.isFolia
import java.io.File

internal lateinit var configFile: File
internal lateinit var config: ConfigScheme
var isFolia: Boolean = false

internal class EnderaLib : JavaPlugin() {

    override fun onEnable() {

        isFolia = isFolia()
        MetricsLite(this, 23669)
        configFile = File("${dataFolder}/config.yml")

        getCommand("enderalib")?.setExecutor(EnderaLibCommand(this))

        val configManager = ConfigurationManager(
            configFile = configFile,
            dataFolder = dataFolder,
            defaultConfig = defaultConfig,
            serializer = ConfigScheme.serializer(),
            logger = logger,
            clazz = ConfigScheme::class
        )

        try {
            val loadedConfig = configManager.loadOrCreateConfig()
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
                    sender.sendMessage("<green>EnderaLib <yellow>version: <gray>${plugin.description.version}".stringToComponent())
                    return true
                } else {
                    sender.sendMessage("<red>This command doesn't exist".stringToComponent())
                }
            }
            return false
        }
    }
}