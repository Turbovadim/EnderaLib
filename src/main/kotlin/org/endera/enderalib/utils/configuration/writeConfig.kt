package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.KSerializer
import java.io.File
import kotlin.reflect.KClass

/**
 * Записывает объект конфигурации в файл в формате YAML, добавляя комментарии.
 *
 * @param T Тип объекта конфигурации.
 * @param file Файл, в который будет записана конфигурация.
 * @param config Объект конфигурации для записи.
 * @param serializer Сериализатор для типа конфигурации.
 * @param clazz Класс типа конфигурации (для замены reified).
 * @param yamlConfiguration Параметры конфигурации YAML (по умолчанию нестрогий режим).
 * @throws Exception при ошибках записи или сериализации.
 */
fun <T : Any> writeConfigWithComments(
    file: File,
    config: T,
    serializer: KSerializer<T>,
    clazz: KClass<T>,
    yamlConfiguration: YamlConfiguration = YamlConfiguration(
        strictMode = false,
        breakScalarsAt = 400,
        yamlNamingStrategy = YamlNamingStrategy.KebabCase
    )
) {
    val yaml = Yaml(configuration = yamlConfiguration)
    val serialized = yaml.encodeToString(serializer, config)
    val withComments = addComments(serialized, clazz)
    file.writeText(withComments)
}