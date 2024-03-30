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
fun <T> configLoadCreationHandler(
    configFile: File,
    dataFolder: File,
    defaultConfig: T,
    serializer: KSerializer<T>,
    logger: Logger,
): T {
    if (!configFile.exists()) {
        try {
            dataFolder.mkdirs()
            writeConfig(configFile, defaultConfig, serializer)
        } catch (e: Exception) {
            logger.severe("Failed to generate config!")
            logger.severe(e.message ?: "Unknown Error")
            throw PluginException("Failed to generate config", e)
        }
    }

    return try {
        val loadedConfig = loadConfig(configFile, serializer)
        logger.info("Configuration successfully loaded!")
        return loadedConfig
    } catch (e: Exception) {
        logger.severe(e.message ?: "Unknown Error")
        logger.severe("Failed to load the configuration!")
        try {
            logger.warning("Trying to generate new configuration")
            renameInvalidConfig(configFile)
            writeConfig(configFile, defaultConfig, serializer)
            loadConfig(configFile, serializer).also {
                logger.info("Configuration successfully updated!")
            }
        } catch (e: Exception) {
            logger.severe("Error when re-generating the configuration!")
            logger.severe(e.message ?: "Unknown Error")
            throw PluginException("Failed to regenerate config", e)
        }
    }
}

/**
 * Exception thrown by plugin operations when critical errors occur, such as failure to load or generate a configuration file.
 *
 * @param message The detail message string of the exception.
 * @param cause The cause of the exception (which is saved for later retrieval by the [Throwable.getCause()] method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
 */
class PluginException(message: String, cause: Throwable? = null) : Exception(message, cause)
