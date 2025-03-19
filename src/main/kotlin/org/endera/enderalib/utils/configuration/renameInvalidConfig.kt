package org.endera.enderalib.utils.configuration

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Renames the provided configuration file by appending a timestamp and marking it as invalid.
 * This is typically used when an existing configuration file is found to be corrupt or cannot be loaded,
 * allowing the system to generate a new configuration file while preserving the old one for debugging or recovery.
 *
 * @param file The file object representing the existing configuration file to be renamed.
 */
fun renameInvalidConfig(file: File) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    val originalName = file.name
    val dotIndex = originalName.lastIndexOf('.')
    val newName = if (dotIndex != -1) {
        // Если есть расширение, то вставляем суффикс перед ним
        val baseName = originalName.substring(0, dotIndex)
        val extension = originalName.substring(dotIndex) // включает точку
        "${baseName}_invalid_${dateFormat.format(Date())}$extension"
    } else {
        // Если расширения нет, просто добавляем суффикс к имени
        "${originalName}_invalid_${dateFormat.format(Date())}"
    }

    val success = file.renameTo(File(file.parent, newName))
    if (!success) {
        println("Не удалось переименовать файл конфигурации.")
    }
}
