package pictures.reisishot.mise.backend.config.tags

import pictures.reisishot.mise.backend.config.TagConfigDsl

typealias NewTag = String
typealias TargetTag = String
typealias AdditionalTagConfig = MutableMap<TargetTag, MutableSet<NewTag>>

@TagConfigDsl
fun buildTagConfig(action: TagConfig.() -> Unit): TagConfig =
    TagConfig().also(action)
