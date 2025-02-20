package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNamingStrategy
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlPath
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.KSerializer

/**
 * Универсальная функция для преобразования объекта конфигурации в YAML-строку.
 */
fun <T> configToYaml(config: T, serializer: KSerializer<T>): String {
    val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false,
            breakScalarsAt = 400,
            yamlNamingStrategy = YamlNamingStrategy.KebabCase
        )
    )
    return yaml.encodeToString(serializer, config)
}

/**
 * Объединяет YAML-представление содержимого файла и дефолтного объекта.
 *
 * @param fileContent Сырые данные файла конфигурации.
 * @param defaultConfig Дефолтный объект конфигурации.
 * @param serializer Сериализатор для типа конфигурации.
 * @return Объект конфигурации типа [T] после объединения.
 */
fun <T> mergeYamlConfigs(
    fileContent: String,
    defaultConfig: T,
    serializer: KSerializer<T>
): T {
    val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false,
            breakScalarsAt = 400,
            yamlNamingStrategy = YamlNamingStrategy.KebabCase
        )
    )
    // Декодируем содержимое файла в динамическое представление
    val fileNode = yaml.decodeFromString(YamlNode.serializer(), fileContent)
    // Кодируем дефолтный объект в YAML и декодируем в динамическое представление
    val defaultYamlString = yaml.encodeToString(serializer, defaultConfig)
    val defaultNode = yaml.decodeFromString(YamlNode.serializer(), defaultYamlString)
    // Объединяем два YAML-дерева
    val mergedNode = mergeYamlNodes(fileNode, defaultNode)
    // Преобразуем объединённое дерево обратно в YAML-строку и декодируем в объект конфигурации
    val mergedYamlString = yaml.encodeToString(YamlNode.serializer(), mergedNode)
    return yaml.decodeFromString(serializer, mergedYamlString)
}

/**
 * Рекурсивно объединяет два YAML-дерева (YamlNode).
 *
 * Если оба узла являются маппингами, для каждого ключа:
 *   - Если значение присутствует в fileNode, объединяет его с дефолтным значением.
 *   - Иначе используется дефолтное значение.
 * Для последовательностей используется значение из fileNode.
 * Для остальных случаев возвращается fileNode.
 */
/**
 * Рекурсивно объединяет два YAML-дерева (YamlNode).
 *
 * Если оба узла являются мапами, для каждого ключа:
 *   - Если значение присутствует в fileNode, объединяет его с дефолтным значением.
 *   - Иначе используется дефолтное значение.
 * Для списков используется значение из fileNode.
 * Для остальных случаев возвращается fileNode.
 */
fun mergeYamlNodes(fileNode: YamlNode, defaultNode: YamlNode): YamlNode {
    return when {
        fileNode is YamlMap && defaultNode is YamlMap -> {
            val mergedEntries = mutableMapOf<YamlScalar, YamlNode>()
            // Обходим ключи дефолтного мапа
            defaultNode.entries.forEach { (key, defaultValue) ->
                val fileValue = fileNode.entries[key]
                mergedEntries[key] = if (fileValue != null) mergeYamlNodes(fileValue, defaultValue) else defaultValue
            }
            // Добавляем дополнительные ключи из fileNode
            fileNode.entries.forEach { (key, fileValue) ->
                if (!mergedEntries.containsKey(key)) {
                    mergedEntries[key] = fileValue
                }
            }
            YamlMap(mergedEntries, path = YamlPath.root)
        }
        fileNode is YamlList && defaultNode is YamlList -> {
            // Для списков используем значение из файла
            fileNode
        }
        else -> fileNode // Для скаляров или несовпадающих типов — используем значение из файла
    }
}
