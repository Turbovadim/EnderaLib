package org.endera.enderalib.utils.configuration

import kotlin.reflect.KClass

@Suppress("unused")
class MultiConfigurationManager(private val managers: List<ConfigurationManager<*>>) {
    fun loadAllConfigs(): Map<KClass<*>, Any> {
        val loadedConfigs = mutableMapOf<KClass<*>, Any>()
        for (manager in managers) {
            loadedConfigs[manager.type] = manager.loadOrCreateConfig()
        }
        return loadedConfigs
    }
}
