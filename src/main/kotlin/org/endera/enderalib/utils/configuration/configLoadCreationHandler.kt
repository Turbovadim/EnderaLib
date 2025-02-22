package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.KSerializer
import java.io.File
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * Класс-менеджер конфигурации, отвечающий за загрузку или создание файла конфигурации,
 * его динамическое слияние с дефолтной конфигурацией и запись с комментариями.
 *
 * @param T Тип объекта конфигурации.
 * @param configFile Файл конфигурации.
 * @param dataFolder Папка с данными плагина.
 * @param defaultConfig Дефолтный объект конфигурации.
 * @param serializer Сериализатор для типа конфигурации.
 * @param logger Логгер для сообщений об ошибках и предупреждениях.
 * @param clazz Класс типа конфигурации (для замены reified).
 */
class ConfigurationManager<T : Any>(
    private val configFile: File,
    private val dataFolder: File,
    private val defaultConfig: T,
    private val serializer: KSerializer<T>,
    private val logger: Logger,
    private val clazz: KClass<T>
) {

    /**
     * Загружает или создаёт конфигурацию, объединяет её с дефолтной и записывает изменения в файл.
     *
     * @return Загруженная или вновь созданная конфигурация.
     * @throws PluginException если не удалось загрузить или создать конфигурацию.
     */
    fun loadOrCreateConfig(): T {
        if (!configFile.exists()) {
            logger.info("Configuration file not found, creating new one.")
            return createNewConfig()
        }

        var fileConfig: T? = null

        // Пытаемся загрузить конфигурацию в строгом режиме
        try {
            fileConfig = loadConfig(
                configFile,
                serializer,
                YamlConfiguration(
                    strictMode = true,
                    breakScalarsAt = 400,
                    yamlNamingStrategy = YamlNamingStrategy.KebabCase
                )
            ).also {
                logger.info("Configuration successfully loaded in strict mode!")
            }
        } catch (e: Exception) {
            logger.warning("Parsing in strict mode failed: ${e.message ?: "Unknown error"}.")
        }

        // Если строгий режим не сработал, выполняем динамическое слияние
        if (fileConfig == null) {
            // Создаем резервную копию исходного файла, так как мерджер не обращает внимания на его содержимое
            try {
                val backupFile = File(
                    configFile.parent,
                    "${configFile.nameWithoutExtension}-backup.${configFile.extension}"
                )
                configFile.copyTo(backupFile, overwrite = true)
                logger.info("Backup of original configuration saved to: ${backupFile.absolutePath}")
            } catch (e: Exception) {
                logger.warning("Failed to backup original configuration: ${e.message ?: "Unknown error"}.")
            }

            try {
                logger.info("Attempting dynamic merge of configuration.")
                val fileContent = configFile.readText(Charsets.UTF_8)
                fileConfig = mergeYamlConfigs(fileContent, defaultConfig, serializer).also {
                    logger.info("Dynamic merge successful!")
                }
            } catch (e: Exception) {
                logger.severe("Dynamic merge failed: ${e.message ?: "Unknown error"}.")
            }
        }

        // Если восстановить конфигурацию не удалось, переименовываем файл и создаём новый
        if (fileConfig == null) {
            logger.warning("Invalid configuration detected. Renaming invalid configuration file and creating a new one.")
            try {
                renameInvalidConfig(configFile)
                return createNewConfig().also {
                    logger.info("Configuration successfully regenerated!")
                }
            } catch (e: Exception) {
                logger.severe("Error while regenerating configuration: ${e.message ?: "Unknown error"}.")
                throw PluginException("Failed to regenerate configuration", e)
            }
        }

        // Для гарантии заполнения всех полей объединяем загруженную конфигурацию с дефолтной
        val mergedConfig: T = mergeYamlConfigs(
            configToYaml(fileConfig, serializer),
            defaultConfig,
            serializer
        )
        // Записываем объединённый конфиг обратно в файл, добавляя комментарии
        writeConfigWithComments(configFile, mergedConfig, serializer, clazz)
        return mergedConfig
    }

    /**
     * Создаёт новый файл конфигурации, используя дефолтную конфигурацию.
     *
     * @return Новый объект конфигурации.
     * @throws PluginException если не удалось создать или загрузить конфигурацию.
     */
    private fun createNewConfig(): T {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw PluginException("Failed to create data folder: ${dataFolder.absolutePath}")
        }
        try {
            writeConfigWithComments(configFile, defaultConfig, serializer, clazz)
        } catch (e: Exception) {
            logger.severe("Error while creating new configuration: ${e.message ?: "Unknown error"}")
            throw PluginException("Failed to create configuration", e)
        }
        return try {
            loadConfig(configFile, serializer).also {
                logger.info("Configuration successfully created and loaded!")
            }
        } catch (e: Exception) {
            logger.severe("Error while loading the created configuration: ${e.message ?: "Unknown error"}")
            throw PluginException("Failed to load configuration after creation", e)
        }
    }
}

/**
 * Исключение, выбрасываемое при критических ошибках загрузки или создания конфигурации.
 */
class PluginException(message: String, cause: Throwable? = null) : Exception(message, cause)