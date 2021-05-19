package pictures.reisishot.mise.backend.generator.pages.overview

data class OverviewConfig(
    val name: String,
    val _style: String?
)

fun OverviewConfig?.computeStyle() = this?._style ?: "default"
