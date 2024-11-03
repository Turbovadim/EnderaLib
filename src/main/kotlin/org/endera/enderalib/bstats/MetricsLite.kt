package org.endera.enderalib.bstats

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.ServicePriority
import org.endera.enderalib.utils.async.BukkitDispatcher
import org.endera.enderalib.utils.async.ioDispatcher
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.Level
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection

class MetricsLite(plugin: Plugin, pluginId: Int) {
    val isEnabled: Boolean

    // The plugin
    private val plugin: Plugin

    private val bukkitDispatcher: BukkitDispatcher = BukkitDispatcher(plugin)

    // The plugin id
    private val pluginId: Int
    /**
     * Class constructor.
     *
     * @param plugin The plugin which stats should be submitted.
     * @param pluginId The id of the plugin.
     * It can be found at [What is my plugin id?](https://bstats.org/what-is-my-plugin-id)
     */
    init {
        this.plugin = plugin
        this.pluginId = pluginId

        val bStatsFolder = File(plugin.dataFolder.parentFile, "bStats")
        val configFile = File(bStatsFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)

        if (!config.isSet("serverUuid")) {

            config.addDefault("enabled", true)
            config.addDefault("serverUuid", UUID.randomUUID().toString())
            config.addDefault("logFailedRequests", false)
            config.addDefault("logSentData", false)
            config.addDefault("logResponseStatusText", false)

            config.options().header(
                """
                    bStats collects some data for plugin authors like how many servers are using their plugins.
                    To honor their work, you should not disable it.
                    This has nearly no effect on the server performance!
                    Check out https://bStats.org/ to learn more :)
                    """.trimIndent()
            ).copyDefaults(true)
            try {
                config.save(configFile)
            } catch (ignored: IOException) {
            }
        }

        // Load the data
        serverUUID = config.getString("serverUuid")
        logFailedRequests = config.getBoolean("logFailedRequests", false)
        isEnabled = config.getBoolean("enabled", true)
        logSentData = config.getBoolean("logSentData", false)
        logResponseStatusText = config.getBoolean("logResponseStatusText", false)
        if (isEnabled) {
            var found = false
            // Search for all other bStats Metrics classes to see if we are the first one
            for (service in Bukkit.getServicesManager().knownServices) {
                try {
                    service.getField("3.0.2") // Our identifier :)
                    found = true // We aren't the first
                    break
                } catch (ignored: NoSuchFieldException) {
                }
            }
            Bukkit.getServicesManager().register(MetricsLite::class.java, this, plugin, ServicePriority.Normal)
            if (!found) {
                startSubmitting()
            }
        }
    }

    /**
     * Starts the Scheduler which submits our data every 30 minutes.
     */
    private fun startSubmitting() {
        val timer = Timer(true) // We use a timer cause the Bukkit scheduler is affected by server lags
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (!plugin.isEnabled) { // Plugin was disabled
                    timer.cancel()
                    return
                }

                CoroutineScope(bukkitDispatcher).launch {
                    submitData()
                    println("ЕБАЛ БСТАТ")
                }

                // Nevertheless we want our code to run in the Bukkit main thread, so we have to use the Bukkit scheduler
                // Don't be afraid! The connection to the bStats server is still async, only the stats collection is sync ;)
            }
        }, (1000 * 60 * 5).toLong(), (1000 * 60 * 30).toLong())
        // Submit the data every 30 minutes, first time after 5 minutes to give other plugins enough time to start
        // WARNING: Changing the frequency has no effect but your plugin WILL be blocked/deleted!
        // WARNING: Just don't do it!
    }

    val pluginData: JsonObject
        get() {
            val data = JsonObject()

            val pluginName = plugin.description.name
            val pluginVersion = plugin.description.version

            data.addProperty("pluginName", pluginName) // Append the name of the plugin
            data.addProperty("id", pluginId) // Append the id of the plugin
            data.addProperty("pluginVersion", pluginVersion) // Append the version of the plugin
            data.add("customCharts", JsonArray())

            return data
        }

    private val serverData: JsonObject
        get() {
            // Minecraft specific data
            var playerAmount: Int
            try {
                // Around MC 1.8 the return type was changed to a collection from an array,
                // This fixes java.lang.NoSuchMethodError: org.bukkit.Bukkit.getOnlinePlayers()Ljava/util/Collection;
                val onlinePlayersMethod = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers")
                playerAmount = if (onlinePlayersMethod.returnType == MutableCollection::class.java)
                    (onlinePlayersMethod.invoke(Bukkit.getServer()) as Collection<*>).size
                else
                    (onlinePlayersMethod.invoke(Bukkit.getServer()) as Array<*>).size
            } catch (e: Exception) {
                playerAmount = Bukkit.getOnlinePlayers().size // Just use the new method if the Reflection failed
            }
            val onlineMode = if (Bukkit.getOnlineMode()) 1 else 0
            val bukkitVersion = Bukkit.getVersion()
            val bukkitName = Bukkit.getName()

            // OS/Java specific data
            val javaVersion = System.getProperty("java.version")
            val osName = System.getProperty("os.name")
            val osArch = System.getProperty("os.arch")
            val osVersion = System.getProperty("os.version")
            val coreCount = Runtime.getRuntime().availableProcessors()

            val data = JsonObject()

            data.addProperty("serverUUID", serverUUID)

            data.addProperty("playerAmount", playerAmount)
            data.addProperty("onlineMode", onlineMode)
            data.addProperty("bukkitVersion", bukkitVersion)
            data.addProperty("bukkitName", bukkitName)

            data.addProperty("javaVersion", javaVersion)
            data.addProperty("osName", osName)
            data.addProperty("osArch", osArch)
            data.addProperty("osVersion", osVersion)
            data.addProperty("coreCount", coreCount)

            return data
        }

    /**
     * Collects the data and sends it afterwards.
     */
    private suspend fun submitData() {
        val data = serverData

        val pluginData = JsonArray()
        // Search for all other bStats Metrics classes to get their plugin data
        for (service in Bukkit.getServicesManager().knownServices) {
            try {
                println("ЕБАЛ БСТАТ2")
                service.getField("B_STATS_VERSION") // Our identifier :)

                for (provider in Bukkit.getServicesManager().getRegistrations(service)) {
                    try {
                        val plugin = provider.service.getMethod("getPluginData").invoke(provider.provider)
                        if (plugin is JsonObject) {
                            pluginData.add(plugin)
                        } else { // old bstats version compatibility
                            try {
                                val jsonObjectJsonSimple = Class.forName("org.json.simple.JSONObject")
                                if (plugin.javaClass.isAssignableFrom(jsonObjectJsonSimple)) {
                                    val jsonStringGetter = jsonObjectJsonSimple.getDeclaredMethod("toJSONString")
                                    jsonStringGetter.isAccessible = true
                                    val jsonString = jsonStringGetter.invoke(plugin) as String
                                    val `object` = JsonParser().parse(jsonString).asJsonObject
                                    pluginData.add(`object`)
                                    println("ЕБАЛ БСТАТ3")
                                }
                            } catch (e: ClassNotFoundException) {
                                // minecraft version 1.14+
                                if (logFailedRequests) {
                                    this.plugin.logger.log(Level.SEVERE, "Encountered unexpected exception ", e)
                                }
                            }
                        }
                    } catch (ignored: NullPointerException) {
                    } catch (ignored: NoSuchMethodException) {
                    } catch (ignored: IllegalAccessException) {
                    } catch (ignored: InvocationTargetException) {
                    }
                }
            } catch (ignored: NoSuchFieldException) {
            }
        }

        data.add("plugins", pluginData)

        withContext(ioDispatcher) {
            try {
                // Send the data
                sendData(plugin, data)
            } catch (e: Exception) {
                // Something went wrong! :(
                if (logFailedRequests) {
                    plugin.logger.log(
                        Level.WARNING,
                        "Could not submit plugin stats of " + plugin.name,
                        e
                    )
                }
            }
        }
    }

    companion object {
        init {
            // You can use the property to disable the check in your test environment
            if (System.getProperty("bstats.relocatecheck") == null || System.getProperty("bstats.relocatecheck") != "false") {
                // Maven's Relocate is clever and changes strings, too. So we have to use this little "trick" ... :D
                val defaultPackage = String(
                    byteArrayOf(
                        'o'.code.toByte(),
                        'r'.code.toByte(),
                        'g'.code.toByte(),
                        '.'.code.toByte(),
                        'b'.code.toByte(),
                        's'.code.toByte(),
                        't'.code.toByte(),
                        'a'.code.toByte(),
                        't'.code.toByte(),
                        's'.code.toByte(),
                        '.'.code.toByte(),
                        'b'.code.toByte(),
                        'u'.code.toByte(),
                        'k'.code.toByte(),
                        'k'.code.toByte(),
                        'i'.code.toByte(),
                        't'.code.toByte()
                    )
                )
                val examplePackage = String(
                    byteArrayOf(
                        'y'.code.toByte(),
                        'o'.code.toByte(),
                        'u'.code.toByte(),
                        'r'.code.toByte(),
                        '.'.code.toByte(),
                        'p'.code.toByte(),
                        'a'.code.toByte(),
                        'c'.code.toByte(),
                        'k'.code.toByte(),
                        'a'.code.toByte(),
                        'g'.code.toByte(),
                        'e'.code.toByte()
                    )
                )
                // We want to make sure nobody just copy & pastes the example and use the wrong package names
                check(!(MetricsLite::class.java.getPackage().name == defaultPackage || MetricsLite::class.java.getPackage().name == examplePackage)) { "bStats Metrics class has not been relocated correctly!" }
            }
        }

        // The version of this bStats class
        const val B_STATS_VERSION: Int = 1

        // The url to which the data is sent
        private const val URL = "https://bStats.org/submitData/bukkit"

        // Should failed requests be logged?
        private var logFailedRequests: Boolean = false

        // Should the sent data be logged?
        private var logSentData: Boolean = false

        // Should the response text be logged?
        private var logResponseStatusText: Boolean = false

        // The uuid of the server
        private var serverUUID: String? = null

        @Throws(Exception::class)
        private fun sendData(plugin: Plugin, data: JsonObject) {
            if (Bukkit.isPrimaryThread()) {
                throw IllegalAccessException("This method must not be called from the main thread!")
            }
            if (logSentData) {
                plugin.logger.info("Sending data to bStats: $data")
            }
            val connection = URL(URL).openConnection() as HttpsURLConnection

            val compressedData = compress(data.toString())

            // Add headers
            connection.requestMethod = "POST"
            connection.addRequestProperty("Accept", "application/json")
            connection.addRequestProperty("Connection", "close")
            connection.addRequestProperty("Content-Encoding", "gzip") // We gzip our request
            connection.addRequestProperty("Content-Length", compressedData!!.size.toString())
            connection.setRequestProperty("Content-Type", "application/json") // We send our data in JSON format
            connection.setRequestProperty("User-Agent", "MC-Server/$B_STATS_VERSION")

            // Send data
            connection.doOutput = true
            DataOutputStream(connection.outputStream).use { outputStream ->
                outputStream.write(compressedData)
            }
            val builder = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream)).use { bufferedReader ->
                var line: String?
                while ((bufferedReader.readLine().also { line = it }) != null) {
                    builder.append(line)
                }
            }
            if (logResponseStatusText) {
                plugin.logger.info("Sent data to bStats and received response: $builder")
            }
        }

        @Throws(IOException::class)
        private fun compress(str: String?): ByteArray? {
            if (str == null) {
                return null
            }
            val outputStream = ByteArrayOutputStream()
            GZIPOutputStream(outputStream).use { gzip ->
                gzip.write(str.toByteArray(StandardCharsets.UTF_8))
            }
            return outputStream.toByteArray()
        }
    }
}