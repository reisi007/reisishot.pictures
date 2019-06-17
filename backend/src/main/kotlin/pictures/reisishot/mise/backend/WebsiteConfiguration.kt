package pictures.reisishot.mise.backend

import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

data class WebsiteConfiguration(
    val title: String,
    val inPath: Path = Paths.get("./src/main/resources"),
    val tmpPath: Path = Paths.get("./src/main/resources/cache"),
    val outPath: Path = Paths.get("./generated"),
    val locale: Locale = Locale.getDefault(),
    val generators: Array<WebsiteGenerator> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WebsiteConfiguration

        if (title != other.title) return false
        if (!generators.contentEquals(other.generators)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + generators.contentHashCode()
        return result
    }
}