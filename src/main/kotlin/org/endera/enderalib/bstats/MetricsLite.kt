package org.endera.enderalib.bstats

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
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
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.Level
import java.util.zip.GZIPOutputStream

class MetricsLite(
    private val plugin: Plugin,
    private val pluginId: Int
) {
    private val isEnabled: Boolean
    private val bukkitDispatcher: BukkitDispatcher = BukkitDispatcher(plugin)

    init {
        val config = loadOrCreateConfig()
        // Загружаем параметры конфигурации
        serverUUID = config.getString("serverUuid")
        logFailedRequests = config.getBoolean("logFailedRequests", false)
        isEnabled = config.getBoolean("enabled", true)
        logSentData = config.getBoolean("logSentData", false)
        logResponseStatusText = config.getBoolean("logResponseStatusText", false)

        if (isEnabled) {
            var found = false
            // Проверка на регистрацию других Metrics-классов
            for (service in Bukkit.getServicesManager().knownServices) {
                try {
                    service.getField("3.0.2") // Наш идентификатор :)
                    found = true
                    break
                } catch (_: NoSuchFieldException) {
                }
            }
            Bukkit.getServicesManager().register(MetricsLite::class.java, this, plugin, ServicePriority.Normal)
            if (!found) {
                startSubmitting()
            }
        }
    }

    /**
     * Загружает или создаёт конфигурационный файл для bStats.
     */
    private fun loadOrCreateConfig(): YamlConfiguration {
        val bStatsFolder = File(plugin.dataFolder.parentFile, "bStats")
        if (!bStatsFolder.exists()) {
            bStatsFolder.mkdirs()
        }
        val configFile = File(bStatsFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true)
            config.addDefault("serverUuid", UUID.randomUUID().toString())
            config.addDefault("logFailedRequests", false)
            config.addDefault("logSentData", false)
            config.addDefault("logResponseStatusText", false)
            config.options().setHeader(
                listOf(
                    "bStats collects some data for plugin authors like how many servers are using their plugins.",
                    "To honor their work, you should not disable it.",
                    "This has nearly no effect on the server performance!",
                    "Check out https://bStats.org/ to learn more :)"
                )
            ).copyDefaults(true)
            try {
                config.save(configFile)
            } catch (_: IOException) {
            }
        }
        return config
    }

    /**
     * Запускает периодическую отправку данных каждые 30 минут (первый запуск через 5 минут).
     */
    private fun startSubmitting() {
        val timer = Timer(true)
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (!plugin.isEnabled) {
                    timer.cancel()
                    return
                }
                CoroutineScope(bukkitDispatcher).launch {
                    submitData()
                }
            }
        }, 1000L * 60 * 5, 1000L * 60 * 30)
    }

    val pluginData: JsonObject
        get() = JsonObject().apply {
            addProperty("pluginName", plugin.description.name)
            addProperty("id", pluginId)
            addProperty("pluginVersion", plugin.description.version)
            add("customCharts", JsonArray())
        }

    private val serverData: JsonObject
        get() = JsonObject().apply {
            addProperty("serverUUID", serverUUID)
            addProperty("playerAmount", getOnlinePlayersCount())
            addProperty("onlineMode", if (Bukkit.getOnlineMode()) 1 else 0)
            addProperty("bukkitVersion", Bukkit.getVersion())
            addProperty("bukkitName", Bukkit.getName())
            addProperty("javaVersion", System.getProperty("java.version"))
            addProperty("osName", System.getProperty("os.name"))
            addProperty("osArch", System.getProperty("os.arch"))
            addProperty("osVersion", System.getProperty("os.version"))
            addProperty("coreCount", Runtime.getRuntime().availableProcessors())
        }

    /**
     * Получает количество онлайн игроков с учётом совместимости разных версий Bukkit.
     */
    private fun getOnlinePlayersCount(): Int {
        return try {
            val onlinePlayersMethod = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers")
            if (onlinePlayersMethod.returnType == MutableCollection::class.java)
                (onlinePlayersMethod.invoke(Bukkit.getServer()) as Collection<*>).size
            else
                (onlinePlayersMethod.invoke(Bukkit.getServer()) as Array<*>).size
        } catch (_: Exception) {
            Bukkit.getOnlinePlayers().size
        }
    }

    /**
     * Собирает данные и отправляет их.
     */
    private suspend fun submitData() {
        val data = serverData
        val pluginsArray = JsonArray()

        // Собираем данные по плагинам, зарегистрированным через bStats
        for (service in Bukkit.getServicesManager().knownServices) {
            try {
                service.getField("B_STATS_VERSION")
                for (provider in Bukkit.getServicesManager().getRegistrations(service)) {
                    try {
                        when (val pluginData = provider.service.getMethod("getPluginData").invoke(provider.provider)) {
                            is JsonObject -> pluginsArray.add(pluginData)
                            else -> {
                                try {
                                    val jsonObjectJsonSimple = Class.forName("org.json.simple.JSONObject")
                                    if (pluginData.javaClass.isAssignableFrom(jsonObjectJsonSimple)) {
                                        val jsonStringGetter = jsonObjectJsonSimple.getDeclaredMethod("toJSONString")
                                        jsonStringGetter.isAccessible = true
                                        val jsonString = jsonStringGetter.invoke(pluginData) as String
                                        val jsonObj = JsonParser().parse(jsonString).asJsonObject
                                        pluginsArray.add(jsonObj)
                                    }
                                } catch (e: ClassNotFoundException) {
                                    if (logFailedRequests) {
                                        plugin.logger.log(
                                            Level.SEVERE,
                                            "Unexpected exception during metrics submission",
                                            e
                                        )
                                    }
                                }
                            }
                        }
                    } catch (_: NullPointerException) {
                    } catch (_: NoSuchMethodException) {
                    } catch (_: IllegalAccessException) {
                    } catch (_: InvocationTargetException) {
                    }
                }
            } catch (_: NoSuchFieldException) {
            }
        }
        data.add("plugins", pluginsArray)

        withContext(ioDispatcher) {
            try {
                sendData(plugin, data)
            } catch (e: Exception) {
                if (logFailedRequests) {
                    plugin.logger.log(Level.WARNING, "Could not submit plugin stats for ${plugin.name}", e)
                }
            }
        }
    }

    companion object {
        init {
            if (System.getProperty("bstats.relocatecheck") == null ||
                System.getProperty("bstats.relocatecheck") != "false"
            ) {
                val defaultPackage = String(
                    byteArrayOf(
                        'o'.code.toByte(), 'r'.code.toByte(), 'g'.code.toByte(), '.'.code.toByte(),
                        'b'.code.toByte(), 's'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(),
                        't'.code.toByte(), 's'.code.toByte(), '.'.code.toByte(), 'b'.code.toByte(),
                        'u'.code.toByte(), 'k'.code.toByte(), 'k'.code.toByte(), 'i'.code.toByte()
                    )
                )
                val examplePackage = String(
                    byteArrayOf(
                        'y'.code.toByte(), 'o'.code.toByte(), 'u'.code.toByte(), 'r'.code.toByte(),
                        '.'.code.toByte(), 'p'.code.toByte(), 'a'.code.toByte(), 'c'.code.toByte(),
                        'k'.code.toByte(), 'a'.code.toByte(), 'g'.code.toByte(), 'e'.code.toByte()
                    )
                )
                check(
                    !(MetricsLite::class.java.getPackage().name == defaultPackage ||
                            MetricsLite::class.java.getPackage().name == examplePackage)
                ) { "bStats Metrics class has not been relocated correctly!" }
            }
        }

        private const val B_STATS_VERSION: Int = 1
        private const val URL = "https://bStats.org/submitData/bukkit"
        private var logFailedRequests: Boolean = false
        private var logSentData: Boolean = false
        private var logResponseStatusText: Boolean = false
        private var serverUUID: String? = null

        /**
         * Отправляет данные на сервер bStats с использованием Ktor HttpClient.
         * Метод должен вызываться вне основного потока.
         */
        @Throws(Exception::class)
        private suspend fun sendData(plugin: Plugin, data: JsonObject) {
            if (Bukkit.isPrimaryThread()) {
                throw IllegalAccessException("This method must not be called from the main thread!")
            }
            if (logSentData) {
                plugin.logger.info("Sending data to bStats: $data")
            }
            val compressedData = compress(data.toString())
            val client = HttpClient(OkHttp) {
                engine {
                    config {
                        followRedirects(true)
                    }
                }
            }
            client.use {
                val response: HttpResponse = it.post(URL) {
                    header("Accept", "application/json")
                    header("Connection", "close")
                    header("Content-Encoding", "gzip")
                    header("Content-Length", compressedData.size.toString())
                    header("Content-Type", "application/json")
                    header("User-Agent", "MC-Server/$B_STATS_VERSION")
                    setBody(ByteArrayContent(compressedData, contentType = ContentType.Application.Json))
                }
                if (logResponseStatusText) {
                    val responseText = response.bodyAsText()
                    plugin.logger.info("Sent data to bStats and received response: $responseText")
                }
            }
        }

        @Throws(IOException::class)
        private fun compress(str: String?): ByteArray {
            val outputStream = ByteArrayOutputStream()
            GZIPOutputStream(outputStream).use { gzip ->
                gzip.write(str?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0))
            }
            return outputStream.toByteArray()
        }
    }
}
