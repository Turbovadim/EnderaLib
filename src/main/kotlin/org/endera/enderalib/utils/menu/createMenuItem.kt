package org.endera.enderalib.utils.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.endera.enderalib.adventure.stringToComponent

fun createMenuItem(
    material: Material,
    name: String,
    lore: List<String>? = null,
    modelData: Int? = null,
    keys: List<ItemNameKey> = listOf(),
): ItemStack {
    val item = ItemStack(material)
    val meta = item.itemMeta as ItemMeta
    if (lore != null) {
        meta.lore(lore.map { ("<!italic><white>$it").stringToComponent() })
    }
    if (modelData != null) {
        meta.setCustomModelData(modelData)
    }

    keys.forEach {
        meta.persistentDataContainer.set(it.namespacedKey, PersistentDataType.STRING, it.value)
    }

    meta.displayName(("<!italic>$name").stringToComponent())
    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
    item.itemMeta = meta
    return item
}

data class ItemNameKey(
    val namespacedKey: NamespacedKey,
    val value: String,
)

fun createMenuHeadItem(
    name: String,
    owner: String,
    lore: List<String>? = null,
    modelData: Int? = null,
    keys: List<ItemNameKey> = listOf()
): ItemStack {

    val item = createMenuItem(
        material = Material.PLAYER_HEAD,
        name = name,
        lore = lore,
        modelData = modelData,
        keys = keys
    )
    val meta = item.itemMeta as SkullMeta

    meta.owningPlayer = Bukkit.getOfflinePlayer(owner)

    item.itemMeta = meta
    return item
}