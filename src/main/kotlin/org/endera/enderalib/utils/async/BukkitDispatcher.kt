package org.endera.enderalib.utils.async

import kotlinx.coroutines.Runnable
import org.bukkit.plugin.Plugin
import org.endera.enderalib.isFolia
import kotlin.coroutines.CoroutineContext

class BukkitDispatcher(private val plugin: Plugin) : AbstractBukkitDispatcher(plugin) {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (isFolia) {
            plugin.server.globalRegionScheduler.execute(plugin, block)
        } else {
            runFallback(block)
        }
    }
}
