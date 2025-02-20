import org.endera.enderalib.utils.configuration.Comment
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.findAnnotation

fun String.toKebabCase(): String =
    this.replace(Regex("([a-z])([A-Z])"), "$1-$2").lowercase()

fun addCommentsForClass(clazz: KClass<*>, yamlText: String, baseIndent: String = ""): String {
    var result = yamlText
    clazz.memberProperties.forEach { property ->
        val comment = property.findAnnotation<Comment>()
        val key = property.name.toKebabCase()
        // Регулярное выражение для поиска строки с ключом и последующего блока с отступом больше baseIndent.
        // Оно ищет строку, начинающуюся с baseIndent, затем key и двоеточие, а потом все строки, которые имеют больший отступ.
        val regex = Regex("(?m)^($baseIndent)($key:.*(?:\n(?!$baseIndent\\S).*)*)")
        result = regex.replace(result) { matchResult ->
            val indent = matchResult.groupValues[1]
            var block = matchResult.groupValues[2]
            // Если тип свойства – вложенная data class, обрабатываем её рекурсивно, увеличивая отступ (предполагается indent = 2 пробела)
            val nestedType = property.returnType.classifier as? KClass<*>
            if (nestedType != null && nestedType.annotations.any { it.annotationClass.simpleName == "Serializable" }) {
                block = addCommentsForClass(nestedType, block, "$baseIndent  ")
            }
            // Если аннотация присутствует, вставляем строку комментария над блоком
            val commentStr = if (comment != null) {
                comment.text
                    .trimIndent()
                    .lines()
                    .joinToString("\n") { "$indent# $it" } + "\n"
            } else ""
            "$commentStr$indent$block"
        }
    }
    return result
}

inline fun <reified T : Any> addComments(yamlText: String): String {
    return addCommentsForClass(T::class, yamlText, "")
}
