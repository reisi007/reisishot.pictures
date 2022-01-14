package pictures.reisishot.mise.backend.generator.pages.overview

import kotlinx.serialization.Serializable

@Serializable
data class OverviewConfig(
    val name: String,
    internal val style: String? = null
)

fun OverviewConfig?.computeStyle() = this?.style ?: "default"
