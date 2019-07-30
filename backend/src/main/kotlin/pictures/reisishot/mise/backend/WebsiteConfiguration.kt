package pictures.reisishot.mise.backend

import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

data class WebsiteConfiguration(
    val shortTitle: String,
    val longTitle: String,
    val websiteLocation: String,
    val inPath: Path = Paths.get("./src/main/resources"),
    val tmpPath: Path = Paths.get("./src/main/resources/cache"),
    val outPath: Path = Paths.get("./generated"),
    val locale: Locale = Locale.getDefault(),
    val generators: List<WebsiteGenerator> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WebsiteConfiguration

        if (shortTitle != other.shortTitle) return false
        if (longTitle != other.longTitle) return false
        if (websiteLocation != other.websiteLocation) return false
        if (inPath != other.inPath) return false
        if (tmpPath != other.tmpPath) return false
        if (outPath != other.outPath) return false
        if (locale != other.locale) return false
        if (generators != other.generators) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shortTitle.hashCode()
        result = 31 * result + longTitle.hashCode()
        result = 31 * result + websiteLocation.hashCode()
        result = 31 * result + inPath.hashCode()
        result = 31 * result + tmpPath.hashCode()
        result = 31 * result + outPath.hashCode()
        result = 31 * result + locale.hashCode()
        result = 31 * result + generators.hashCode()
        return result
    }
}