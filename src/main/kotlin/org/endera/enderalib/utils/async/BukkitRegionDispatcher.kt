package org.endera.enderalib.utils.async

import kotlinx.coroutines.Runnable
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.isFolia
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
class BukkitRegionDispatcher(private val plugin: JavaPlugin, private val location: Location) : AbstractBukkitDispatcher(plugin) {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (isFolia) {
            plugin.server.regionScheduler.execute(plugin, location, block)
        } else {
            runFallback(block)
        }
    }
}
