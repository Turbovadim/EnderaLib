package org.endera.enderalib.utils.async

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.isFolia
import kotlin.coroutines.CoroutineContext

class BukkitRegionDispatcher(private val plugin: JavaPlugin, private val location: Location) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (isFolia) {
            plugin.server.regionScheduler.execute(plugin, location, block)
        } else {
            if (Bukkit.isPrimaryThread()) {
                block.run()
            } else {
                Bukkit.getScheduler().runTask(plugin, block)
            }
        }
    }
}
