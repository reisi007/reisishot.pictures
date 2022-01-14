package at.reisishot.mise.backend.config

data class WebsiteConfigBuilder(
    val paths: PathInformation,
    val websiteInformation: GeneralWebsiteInformation,
    val miseConfig: MiseConfig,
    val additionalConfig: MutableMap<String, Any>,
    val generators: MutableList<WebsiteGenerator>
)

fun WebsiteConfigBuilder.generate() = WebsiteConfig(paths, websiteInformation, miseConfig, additionalConfig, generators)

@DslMarker
annotation class WebsiteConfigBuilderDsl

@WebsiteConfigBuilderDsl
fun buildWebsiteConfig(
    paths: PathInformation,
    websiteInformation: GeneralWebsiteInformation,
    miseConfig: MiseConfig,
    builderAction: WebsiteConfigBuilder.() -> Unit
): WebsiteConfig =
    WebsiteConfigBuilder(paths, websiteInformation, miseConfig, mutableMapOf(), mutableListOf())
        .apply(builderAction)
        .generate()

