package org.endera.enderalib.utils.configuration

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

fun String.toKebabCase(): String =
    this.replace(Regex("([a-z])([A-Z])"), "$1-$2").lowercase()

fun addCommentsForClass(clazz: KClass<*>, yamlText: String, baseIndent: String = ""): String {
    var result = yamlText
    clazz.memberProperties.forEach { property ->
        // Получаем аннотации для комментария и spacer.
        val comment = property.findAnnotation<Comment>()
        val spacer = property.findAnnotation<Spacer>()
        val key = property.name.toKebabCase()

        // Регулярное выражение для поиска строки с ключом и последующего блока с отступом больше baseIndent.
        val regex = Regex("(?m)^($baseIndent)($key:.*(?:\n(?!$baseIndent\\S).*)*)")
        result = regex.replace(result) { matchResult ->
            val indent = matchResult.groupValues[1]
            var block = matchResult.groupValues[2]

            // Если тип свойства – вложенная data class, обрабатываем её рекурсивно, увеличивая отступ (например, на 2 пробела)
            val nestedType = property.returnType.classifier as? KClass<*>
            if (nestedType != null && nestedType.annotations.any { it.annotationClass.simpleName == "Serializable" }) {
                block = addCommentsForClass(nestedType, block, "$baseIndent  ")
            }

            // Формирование комментариев, если присутствует аннотация @Comment.
            val commentStr = if (comment != null) {
                comment.text
                    .trimIndent()
                    .lines()
                    .joinToString("\n") { "$indent# $it" } + "\n"
            } else ""

            // Формирование N пустых строк, если есть аннотация @Spacer.
            val spacerStr = spacer?.let { "\n".repeat(it.count) } ?: ""

            // Собираем итоговый блок: сначала пустые строки, затем комментарий, потом само свойство.
            "$spacerStr$commentStr$indent$block"
        }
    }
    return result
}

/**
 * Adds comments to the YAML text using information from class annotations.
 *
 * @param T The type of the configuration object.
 * @param yamlText The original YAML text.
 * @param clazz The configuration type class.
 * @return YAML text with added comments.
 */
fun <T : Any> addComments(yamlText: String, clazz: KClass<T>): String {
    return addCommentsForClass(clazz, yamlText, "")
}