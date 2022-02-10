package pictures.reisishot.mise.backend.config

import pictures.reisishot.mise.commons.FileExtension
import pictures.reisishot.mise.commons.isJetbrainsTemp
import pictures.reisishot.mise.commons.isTemp
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale

data class WebsiteConfig(
    val paths: PathInformation,
    val websiteInformation: GeneralWebsiteInformation,
    val miseConfig: MiseConfig,
    val additionalConfig: Map<String, Any>,
    val generators: List<WebsiteGenerator>
)

data class PathInformation(
    val sourceFolder: Path = Paths.get("./src/main/resources"),
    val cacheFolder: Path = Paths.get("./src/main/resources/cache"),
    val targetFolder: Path = Paths.get("./generated"),
)

class GeneralWebsiteInformation(
    val shortTitle: String,
    val longTitle: String,
    websiteLocation: String,
    override val locale: Locale = Locale.getDefault(),

    ) : LocaleProvider {
    val normalizedWebsiteLocation: String by lazy { websiteLocation.let { if (websiteLocation.endsWith("/")) it else "$it/" } }
}

data class MiseConfig(
    val isDevMode: Boolean = false,
    val cleanupGeneration: Boolean = false,
    private val baseInteractiveDelayMs: Long? = 2000L,
    val interactiveIgnoredFiles: List<(FileExtension) -> Boolean> = listOf(
        FileExtension::isJetbrainsTemp,
        FileExtension::isTemp
    ),
) {
    val interactiveDelayMs: Long?
        get() = if (isDevMode) baseInteractiveDelayMs else null
}
