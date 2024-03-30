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
    val backupFileName = "config_invalid_${dateFormat.format(Date())}.yml"
    val success = file.renameTo(File(file.parent, backupFileName))

    if (!success) {
        println("Failed to rename the configuration file.")
    }
}