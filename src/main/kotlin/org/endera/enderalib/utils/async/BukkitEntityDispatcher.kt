package org.endera.enderalib.utils.async

import kotlinx.coroutines.Runnable
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.isFolia
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
class BukkitEntityDispatcher(private val plugin: JavaPlugin, private val entity: Entity) : AbstractBukkitDispatcher(plugin) {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (isFolia) {
            entity.scheduler.execute(plugin, block, null, 0 )
        } else {
            runFallback(block)
        }
    }
}
