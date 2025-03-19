package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.*
import kotlinx.serialization.KSerializer

/**
 * Merges the YAML representation of the file content and the default object.
 *
 * @param fileContent Raw data from the configuration file.
 * @param defaultConfig The default configuration object.
 * @param serializer The serializer for the configuration type.
 * @return A configuration object of type [T] after merging.
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
 * Recursively merges two YAML trees (YamlNode).
 *
 * If both nodes are maps, for each key:
 *   - If the value is present in fileNode, it merges it with the default value.
 *   - Otherwise, the default value is used.
 * For lists, the value from fileNode is used.
 * In other cases, fileNode is returned.
 */
fun mergeYamlNodes(fileNode: YamlNode, defaultNode: YamlNode): YamlNode {
    return when {
        fileNode is YamlMap && defaultNode is YamlMap -> {
            val mergedEntries = mutableMapOf<YamlScalar, YamlNode>()
            defaultNode.entries.forEach { (defaultKey, defaultValue) ->
                val matchingFileKey = fileNode.entries.keys.find { it.content == defaultKey.content }
                val fileValue = matchingFileKey?.let { fileNode.entries[it] }
                mergedEntries[defaultKey] = if (fileValue != null) mergeYamlNodes(fileValue, defaultValue) else defaultValue
            }
            fileNode.entries.forEach { (fileKey, fileValue) ->
                if (!mergedEntries.keys.any { it.content == fileKey.content }) {
                    mergedEntries[fileKey] = fileValue
                }
            }
            YamlMap(mergedEntries, path = YamlPath.root)
        }
        fileNode is YamlList && defaultNode is YamlList -> {
            fileNode
        }
        else -> fileNode
    }
}
