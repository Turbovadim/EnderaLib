package org.endera.enderalib.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags

val minimessage: MiniMessage = MiniMessage.builder()
    .tags(
        TagResolver.builder()
            .resolver(StandardTags.color())
            .resolver(StandardTags.decorations())
            .resolver(StandardTags.clickEvent())
            .resolver(StandardTags.gradient())
            .resolver(StandardTags.defaults())
            .build()
    )
    .build()

fun String.stringToComponent(): Component {
    return minimessage.deserialize(this)
}

fun Component.componentToString(): String {
    return minimessage.serialize(this)
}