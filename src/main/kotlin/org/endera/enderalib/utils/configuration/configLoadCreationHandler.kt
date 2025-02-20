package org.endera.enderalib.utils.configuration

import kotlinx.serialization.KSerializer
import java.io.File
import java.util.logging.Logger

/**
 * Handles the loading or creation of a plugin configuration file.
 * If the configuration file does not exist, it will attempt to create the file and write the default configuration.
 * If loading the existing configuration fails, it will attempt to rename the invalid configuration file and regenerate it from the default configuration.
 *
 * @param T The type of the configuration object.
 * @param configFile The configuration file to be loaded or created.
 * @param dataFolder The data folder of the plugin, used to ensure its existence for config file creation.
 * @param defaultConfig The default configuration instance to be used if the config file doesn't exist or needs to be recreated.
 * @param serializer The serializer for the configuration object, used for reading from and writing to the file.
 * @param logger The logger used to log errors or warnings during the operation.
 * @return The loaded or newly created configuration object of type [T].
 * @throws PluginException if the configuration file cannot be generated or re-generated upon failure to load.
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

    return try {
        loadConfig(configFile, serializer).also {
            logger.info("Configuration successfully loaded!")
        }
    } catch (e: Exception) {
        logger.severe("Error while loading configuration: ${e.message ?: "Unknown error"}")
        logger.warning("Attempting to rename invalid configuration file and create a new one.")
        try {
            renameInvalidConfig(configFile)
            return createNewConfig(configFile, dataFolder, defaultConfig, serializer, logger).also {
                logger.info("Configuration successfully updated!")
            }
        } catch (e2: Exception) {
            logger.severe("Error while regenerating configuration: ${e2.message ?: "Unknown error"}")
            throw PluginException("Failed to regenerate configuration", e2)
        }
    }
}

/**
 * Creates a new configuration file using the default configuration.
 *
 * @param T The type of the configuration object.
 * @param configFile The configuration file to be created.
 * @param dataFolder The data folder of the plugin, used to ensure its existence.
 * @param defaultConfig The default configuration instance.
 * @param serializer The serializer for the configuration object.
 * @param logger The logger used for logging messages.
 * @return The newly created and loaded configuration object of type [T].
 * @throws PluginException if creation or loading fails.
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
