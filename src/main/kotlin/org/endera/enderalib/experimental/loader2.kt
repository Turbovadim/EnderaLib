//package org.endera.enderalib.experimental
//
//import kotlinx.serialization.KSerializer
//import org.endera.enderalib.utils.configuration.PluginException
//import org.endera.enderalib.utils.configuration.loadConfig
//import org.endera.enderalib.utils.configuration.renameInvalidConfig
//import org.endera.enderalib.utils.configuration.writeConfig
//import java.io.File
//import java.io.IOException
//import java.util.logging.Logger
//
//fun <T> configLoadCreationHandler(
//    configFile: File,
//    dataFolder: File,
//    defaultConfig: T,
//    serializer: KSerializer<T>,
//    logger: Logger,
//): T {
//    if (!configFile.exists()) {
//        try {
//            dataFolder.mkdirs()
//            writeConfig(configFile, defaultConfig, serializer)
//        } catch (e: Exception) {
//            logger.logSevere(e, "Failed to generate config!")
//            throw PluginException("Failed to generate config", e)
//        }
//    }
//
//    return try {
//        loadConfig(configFile, serializer).also {
//            logger.info("Configuration successfully loaded!")
//        }
//    } catch (e: Exception) {
//        logger.logSevere(e, "Failed to load the configuration!")
//        try {
//            logger.warning("Trying to generate new configuration")
//            renameInvalidConfig(configFile)
//            writeConfig(configFile, defaultConfig, serializer)
//            loadConfig(configFile, serializer).also {
//                logger.info("Configuration successfully updated!")
//            }
//        } catch (e: Exception) {
//            logger.logSevere(e, "Error when re-generating the configuration!")
//            throw PluginException("Failed to regenerate config", e)
//        }
//    }
//}
//
//private fun Logger.logSevere(e: Exception, message: String) {
//    severe(message)
//    severe(e.message ?: "Unknown Error")
//}