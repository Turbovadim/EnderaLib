package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.KSerializer
import java.io.File
import kotlin.reflect.KClass

/**
 * Writes the configuration object to a file in YAML format, adding comments.
 *
 * @param T The type of the configuration object.
 * @param file The file where the configuration will be written.
 * @param config The configuration object to write.
 * @param serializer The serializer for the configuration type.
 * @param clazz The configuration type class (for replacing reified).
 * @param yamlConfiguration YAML configuration settings (non-strict mode by default).
 * @throws Exception in case of writing or serialization errors.
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