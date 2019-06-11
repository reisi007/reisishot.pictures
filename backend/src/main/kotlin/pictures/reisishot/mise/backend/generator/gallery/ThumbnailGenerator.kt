package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Path

class ThumbnailGenerator : WebsiteGenerator {

    enum class ImageSize(val longestSidePx: Int) {
        SMALL(300), MEDIUM(1000), LARGE(2500)
    }

    override val executionPriority: Int = 1_000
    override val generatorName: String = "Reisishot JPG Thumbnail generator"
    private val extensionSuffix = Regex("(jpg|jpeg)", RegexOption.IGNORE_CASE)

    override fun isGenerationNeeded(p: Path, extension: String): Boolean = extension.matches(extensionSuffix)

    override fun generate(
        filename: List<Path>,
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        //TODO Add logic
    }
}