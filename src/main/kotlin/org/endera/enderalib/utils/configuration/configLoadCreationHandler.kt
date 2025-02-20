package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.KSerializer
import java.io.File
import java.util.logging.Logger

/**
 * Loads or creates a plugin configuration file and merges it with the default configuration.
 *
 * The function first attempts to load the configuration in strict mode. If that fails,
 * it falls back to non-strict mode. Once loaded, the configuration is merged with the default
 * configuration to ensure that any missing properties (including in nested objects) are filled in.
 * If loading fails altogether, the invalid configuration file is renamed and a new one is generated.
 *
 * @param T The type of the configuration object.
 * @param configFile The configuration file to load or create.
 * @param dataFolder The plugin's data folder, ensuring it exists for file creation.
 * @param defaultConfig The default configuration instance used when creating or regenerating the configuration file.
 * @param serializer The serializer for reading from and writing to the configuration file.
 * @param logger The logger used for reporting errors and warnings during the process.
 * @return The loaded or newly created (and merged) configuration object of type [T].
 * @throws PluginException if the configuration file cannot be generated or regenerated.
 */
inline fun <reified T : Any> configLoadCreationHandler(
    configFile: File,
    dataFolder: File,
    defaultConfig: T,
    serializer: KSerializer<T>,
    logger: Logger,
): T {
    if (!configFile.exists()) {
        logger.info("Configuration file not found, creating new one.")
        return createNewConfig(configFile, dataFolder, defaultConfig, serializer, logger)
    }

    var fileConfig: T? = null
    // Пытаемся загрузить в строгом режиме
    try {
        fileConfig = loadConfig(
            configFile, serializer,
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

    // Если строгое чтение не удалось, выполняем динамическое слияние
    if (fileConfig == null) {
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

    // Если не удалось восстановить конфигурацию, переименовываем файл и создаём новый
    if (fileConfig == null) {
        logger.warning("Renaming invalid configuration file and creating a new one.")
        try {
            renameInvalidConfig(configFile)
            return createNewConfig(configFile, dataFolder, defaultConfig, serializer, logger).also {
                logger.info("Configuration successfully regenerated!")
            }
        } catch (e: Exception) {
            logger.severe("Error while regenerating configuration: ${e.message ?: "Unknown error"}.")
            throw PluginException("Failed to regenerate configuration", e)
        }
    }

    // Для гарантии, что в объекте нет отсутствующих полей, выполняем динамическое объединение
    // даже если конфигурация была успешно загружена
    val mergedConfig: T = mergeYamlConfigs(
        configToYaml(fileConfig, serializer),
        defaultConfig,
        serializer
    )
    // Записываем объединённый конфиг обратно в файл
    writeConfigWithComments(configFile, mergedConfig, serializer)
    return mergedConfig
}

/**
 * Создаёт новый файл конфигурации, используя дефолтную конфигурацию.
 */
inline fun <reified T : Any> createNewConfig(
    configFile: File,
    dataFolder: File,
    defaultConfig: T,
    serializer: KSerializer<T>,
    logger: Logger,
): T {
    if (!dataFolder.exists() && !dataFolder.mkdirs()) {
        throw PluginException("Failed to create data folder: ${dataFolder.absolutePath}")
    }
    try {
        writeConfigWithComments(configFile, defaultConfig, serializer)
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
/**
 * Exception thrown by plugin operations when critical errors occur, such as failure to load or generate a configuration file.
 *
 * @param message The detail message string of the exception.
 * @param cause The cause of the exception (which is saved for later retrieval by the Throwable.getCause() method).
 */
class PluginException(message: String, cause: Throwable? = null) : Exception(message, cause)
