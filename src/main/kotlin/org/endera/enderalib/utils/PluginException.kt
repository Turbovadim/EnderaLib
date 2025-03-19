package org.endera.enderalib.utils

/**
 * Exception thrown in case of critical errors during configuration loading or creation.
 */
class PluginException(message: String, cause: Throwable? = null) : Exception(message, cause)