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
//fun <T> configLoadCreationHandler1(
//    configFile: File,
//    dataFolder: File,
//    defaultConfig: T,
//    serializer: KSerializer<T>,
//    logger: Logger
//): T {
//    if (!configFile.exists()) {
//        try {
//            dataFolder.mkdirs()
//            writeConfig(configFile, defaultConfig, serializer)
//        } catch (e: IOException) {
//            logger.severe("Не удалось сгенерировать конфигурацию: ${configFile.name}")
//            e.printStackTrace()
//            throw PluginException("Не удалось сгенерировать конфигурацию", e)
//        }
//    }
//
//    return try {
//        val loadedConfig = loadConfig(configFile, serializer)
//        logger.info("Конфигурация успешно загружена из файла ${configFile.name}!")
//        loadedConfig
//    } catch (e: Exception) {
//        logger.severe("Не удалось загрузить конфигурацию: ${configFile.name}")
//        e.printStackTrace()
//        try {
//            logger.warning("Попытка сгенерировать новый файл конфигурации.")
//            renameInvalidConfig(configFile)
//            writeConfig(configFile, defaultConfig, serializer)
//            loadConfig(configFile, serializer).also {
//                logger.info("Конфигурация успешно обновлена!")
//            }
//        } catch (e: IOException) {
//            logger.severe("Ошибка при повторной генерации конфигурации: ${configFile.name}")
//            e.printStackTrace()
//            throw PluginException("Не удалось повторно сгенерировать конфигурацию", e)
//        }
//    }
//}