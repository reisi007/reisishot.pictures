package pictures.reisishot.mise.backend

import at.reisishot.mise.commons.FileExtension
import kotlinx.html.DIV
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class WebsiteConfiguration(
        val shortTitle: String,
        val longTitle: String,
        websiteLocation: String,
        private val form: DIV.(target: Path, websiteConfiguration: WebsiteConfiguration) -> Unit,
        val inPath: Path = Paths.get("./src/main/resources"),
        val tmpPath: Path = Paths.get("./src/main/resources/cache"),
        val outPath: Path = Paths.get("./generated"),
        val locale: Locale = Locale.getDefault(),
        val cleanupGeneration: Boolean = false,
        val interactiveDelayMs: Long? = 2000,
        val analyticsSiteId: String? = null,
        val socialMediaLinks: SocialMediaAccounts? = null,
        val generators: List<WebsiteGenerator> = emptyList(),

        vararg val interactiveIgnoredFiles: ((FileExtension) -> Boolean) = arrayOf({ _: String -> false })
) {
    fun createForm(parent: DIV, target: Path) = form(parent, target, this@WebsiteConfiguration)
    val websiteLocation: String = websiteLocation.let { if (websiteLocation.endsWith("/")) it else "$it/" }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WebsiteConfiguration

        if (shortTitle != other.shortTitle) return false
        if (longTitle != other.longTitle) return false
        if (inPath != other.inPath) return false
        if (tmpPath != other.tmpPath) return false
        if (outPath != other.outPath) return false
        if (locale != other.locale) return false
        if (cleanupGeneration != other.cleanupGeneration) return false
        if (interactiveDelayMs != other.interactiveDelayMs) return false
        if (generators != other.generators) return false
        if (!interactiveIgnoredFiles.contentEquals(other.interactiveIgnoredFiles)) return false
        if (websiteLocation != other.websiteLocation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shortTitle.hashCode()
        result = 31 * result + longTitle.hashCode()
        result = 31 * result + inPath.hashCode()
        result = 31 * result + tmpPath.hashCode()
        result = 31 * result + outPath.hashCode()
        result = 31 * result + locale.hashCode()
        result = 31 * result + cleanupGeneration.hashCode()
        result = 31 * result + (interactiveDelayMs?.hashCode() ?: 0)
        result = 31 * result + generators.hashCode()
        result = 31 * result + interactiveIgnoredFiles.contentHashCode()
        result = 31 * result + websiteLocation.hashCode()
        return result
    }

    override fun toString(): String {
        return "WebsiteConfiguration(shortTitle='$shortTitle', longTitle='$longTitle', inPath=$inPath, tmpPath=$tmpPath, outPath=$outPath, locale=$locale, cleanupGeneration=$cleanupGeneration, interactiveDelayMs=$interactiveDelayMs, generators=$generators, interactiveIgnoredFiles=${interactiveIgnoredFiles.contentToString()}, websiteLocation='$websiteLocation')"
    }
}

data class SocialMediaAccounts(
        val facebook: String? = null,
        val instagram: String? = null,
        val mail: String? = null,
        val whatsapp: String? = null
)