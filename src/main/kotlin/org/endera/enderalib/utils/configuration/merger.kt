//import com.charleskorn.kaml.Yaml
//import com.charleskorn.kaml.YamlConfiguration
//import com.charleskorn.kaml.YamlNamingStrategy
//import kotlinx.serialization.KSerializer
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonElement
//import kotlinx.serialization.json.JsonObject
//import java.io.File
//import java.util.logging.Logger
//
//// -----------------------
//// 1. Определение конфигурации
//// -----------------------
//
//@Serializable
//data class MyConfig(
//    val field1: String = "defaultValue1",
//    val field2: Int = 42,
//    val newField: Boolean = true,
//    val nested: NestedConfig = NestedConfig()
//)
//
//@Serializable
//data class NestedConfig(
//    val nestedField: String = "nestedDefault"
//)
//
//// -----------------------
//// 2. Функция для рекурсивного мерджинга JsonObject
//// -----------------------
//
///**
// * Рекурсивно объединяет два [JsonObject]:
// * для каждого ключа берётся значение из [oldObj], если оно присутствует, иначе используется значение из [defaultObj].
// */
//fun mergeJsonObjects(defaultObj: JsonObject, oldObj: JsonObject): JsonObject {
//    val merged = mutableMapOf<String, JsonElement>()
//    // Проходим по всем ключам дефолтного объекта
//    for ((key, defaultValue) in defaultObj) {
//        val oldValue = oldObj[key]
//        merged[key] = if (oldValue != null) {
//            // Если оба значения – объекты, объединяем их рекурсивно
//            if (defaultValue is JsonObject && oldValue is JsonObject) {
//                mergeJsonObjects(defaultValue, oldValue)
//            } else {
//                // В остальных случаях берём значение из старой конфигурации
//                oldValue
//            }
//        } else {
//            // Если в старой конфигурации ключ отсутствует, используем дефолтное значение
//            defaultValue
//        }
//    }
//    return JsonObject(merged)
//}
//
///**
// * Объединяет [defaultConfig] и [oldConfig] для типа [T] с использованием JSON-представления.
// * Если значение присутствует в старом конфиге, оно будет использовано, а отсутствующие — взяты из дефолтного.
// */
//fun <T> mergeConfigs(
//    defaultConfig: T,
//    oldConfig: T,
//    serializer: KSerializer<T>
//): T {
//    val json = Json { ignoreUnknownKeys = true }
//    val defaultJsonElement = json.encodeToJsonElement(serializer, defaultConfig)
//    val oldJsonElement = json.encodeToJsonElement(serializer, oldConfig)
//    if (defaultJsonElement is JsonObject && oldJsonElement is JsonObject) {
//        val mergedJson = mergeJsonObjects(defaultJsonElement, oldJsonElement)
//        return json.decodeFromJsonElement(serializer, mergedJson)
//    } else {
//        // Если конфигурация не представлена в виде объекта – возвращаем старую версию
//        return oldConfig
//    }
//}
//
//// -----------------------
//// 3. Функции для работы с YAML (чтение и запись)
//// -----------------------
//
//private val yamlConfig = YamlConfiguration(
//    strictMode = true,
//    breakScalarsAt = 400,
//    yamlNamingStrategy = YamlNamingStrategy.KebabCase
//)
//
//fun <T> loadYamlConfig(file: File, serializer: KSerializer<T>): T {
//    val yaml = Yaml(configuration = yamlConfig)
//    val text = file.readText(Charsets.UTF_8)
//    return yaml.decodeFromString(serializer, text)
//}
//
//fun <T> writeYamlConfig(file: File, config: T, serializer: KSerializer<T>) {
//    val yaml = Yaml(configuration = yamlConfig)
//    val text = yaml.encodeToString(serializer, config)
//    file.writeText(text)
//}
//
//// -----------------------
//// 4. Обработчик загрузки и мерджинга конфигурации
//// -----------------------
//
///**
// * Загружает конфигурацию из файла, выполняет мерджинг с дефолтной конфигурацией,
// * обновляет файл и возвращает итоговую конфигурацию.
// *
// * Если файл конфигурации отсутствует, он будет создан с дефолтными значениями.
// */
//fun <T> configLoadMergeHandler(
//    configFile: File,
//    dataFolder: File,
//    defaultConfig: T,
//    serializer: KSerializer<T>,
//    logger: Logger
//): T {
//    // Если файл не существует, создаём его с дефолтной конфигурацией
//    if (!configFile.exists()) {
//        try {
//            dataFolder.mkdirs()
//            writeYamlConfig(configFile, defaultConfig, serializer)
//            logger.info("Default configuration generated at ${configFile.absolutePath}")
//            return defaultConfig
//        } catch (e: Exception) {
//            logger.severe("Failed to generate config: ${e.message}")
//            throw Exception("Failed to generate config", e)
//        }
//    }
//
//    // Пытаемся загрузить старую конфигурацию
//    val oldConfig = try {
//        loadYamlConfig(configFile, serializer)
//    } catch (e: Exception) {
//        logger.severe("Failed to load config: ${e.message}")
//        throw Exception("Failed to load config", e)
//    }
//
//    // Мерджим старую конфигурацию с дефолтной
//    val mergedConfig = mergeConfigs(defaultConfig, oldConfig, serializer)
//    // Обновляем файл объединённой конфигурацией
//    try {
//        writeYamlConfig(configFile, mergedConfig, serializer)
//        logger.info("Configuration merged and updated at ${configFile.absolutePath}")
//    } catch (e: Exception) {
//        logger.severe("Failed to write merged config: ${e.message}")
//    }
//    return mergedConfig
//}
//
//// -----------------------
//// 5. Пример использования
//// -----------------------
//
//fun test() {
//    val logger = Logger.getLogger("ConfigLogger")
//    val dataFolder = File("config")
//    val configFile = File(dataFolder, "config.yml")
//
//    // Дефолтная конфигурация (с новыми полями и значениями по умолчанию)
//    val defaultConfig = MyConfig()
//
//    // Загружаем конфигурацию с мерджингом
//    val mergedConfig = configLoadMergeHandler(
//        configFile,
//        dataFolder,
//        defaultConfig,
//        MyConfig.serializer(),
//        logger
//    )
//
//    println("Merged config: $mergedConfig")
//}
