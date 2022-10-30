package pictures.reisinger.config.ui

import kotlinx.serialization.Serializable

@Serializable
data class ImmutableImageConfig(val title: String = "", val tags: Set<String> = emptySet())
