package pictures.reisishot.mise.backend

import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Path
import java.nio.file.Paths

data class WebsiteConfiguration(
    val title: String,
    val inFolder: Path = Paths.get("./src/main/resources"),
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