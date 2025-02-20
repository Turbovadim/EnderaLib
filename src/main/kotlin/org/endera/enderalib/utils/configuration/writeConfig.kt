package org.endera.enderalib.utils.configuration

import addComments
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.KSerializer
import java.io.File

/**
 * Writes the given configuration object to a file in YAML format.
 *
 * @param T The type of the configuration object.
 * @param file The file where the configuration will be written.
 * @param config The configuration object to be written.
 * @param serializer The serializer for the configuration object type.
 * @param yamlConfiguration Optional. The YAML configuration. Defaults to non-strict mode.
 * @throws Exception on file writing or serialization errors.
 */
inline fun <reified T : Any> writeConfigWithComments(
    file: File,
    config: T,
    serializer: KSerializer<T>,
    yamlConfiguration: YamlConfiguration = YamlConfiguration(
        strictMode = false,
        breakScalarsAt = 400,
        yamlNamingStrategy = YamlNamingStrategy.KebabCase
    )
) {
    val yaml = Yaml(configuration = yamlConfiguration)
    val serialized = yaml.encodeToString(serializer, config)
    val withComments = addComments<T>(serialized)
    file.writeText(withComments)
}
