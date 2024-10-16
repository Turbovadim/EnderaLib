package org.endera.enderalib.utils.async

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.isFolia
import kotlin.coroutines.CoroutineContext

class BukkitDispatcher(private val plugin: JavaPlugin) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (isFolia) {
            plugin.server.globalRegionScheduler.execute(plugin, block)
        } else {
            if (Bukkit.isPrimaryThread()) {
                block.run()
            } else {
                Bukkit.getScheduler().runTask(plugin, block)
            }
        }
    }
}
