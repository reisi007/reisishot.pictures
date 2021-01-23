package pictures.reisishot.mise.backend

import at.reisishot.mise.commons.FileExtension
import kotlinx.html.A
import kotlinx.html.DIV
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.YamlMetaDataConsumer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class WebsiteConfiguration(
        val shortTitle: String,
        val longTitle: String,
        websiteLocation: String,
        val inPath: Path = Paths.get("./src/main/resources"),
        val tmpPath: Path = Paths.get("./src/main/resources/cache"),
        val outPath: Path = Paths.get("./generated"),
        val locale: Locale = Locale.getDefault(),
        val cleanupGeneration: Boolean = false,
        private val _interactiveDelayMs: Long? = 2000L,
        private val form: DIV.(target: Path, websiteConfiguration: WebsiteConfiguration) -> Unit = { _, _ -> },
        val analyticsSiteId: String? = null,
        val socialMediaLinks: SocialMediaAccounts? = null,
        val metaDataConsumers: Array<YamlMetaDataConsumer> = emptyArray(),
        val generators: List<WebsiteGenerator> = emptyList(),
        val isDevMode: Boolean = false,
        val cssFileName: String = "styles.css",
        val bootsrapMenuBreakpoint: String = "md",
        val fbMessengerChatPlugin: FacebookMessengerChatPlugin? = null,
        val navbarBrandFunction: A.(WebsiteConfiguration, AbstractGalleryGenerator) -> Unit = { config, _ -> text(config.shortTitle) },
        val interactiveIgnoredFiles: Array<((FileExtension) -> Boolean)> = arrayOf({ _: String -> false })
) {
    val interactiveDelayMs: Long?
        get() = if (isDevMode) _interactiveDelayMs else null

    val metaDataConsumerFileExtensions by lazy {
        metaDataConsumers.asSequence()
                .flatMap { it.interestingFileExtensions() }
                .toList()
                .toTypedArray()
    }

    fun createForm(parent: DIV, target: Path) = form(parent, target, this@WebsiteConfiguration)
    val normalizedWebsiteLocation: String = websiteLocation.let { if (websiteLocation.endsWith("/")) it else "$it/" }


}

data class SocialMediaAccounts(
        val facebook: String? = null,
        val instagram: String? = null,
        val mail: String? = null,
        val whatsapp: String? = null,
        val podcast: String? = null
)

data class FacebookMessengerChatPlugin(
        val pageId: Long,
        val themeColor: String,
        val message: String
)