package pictures.reisishot.mise.backend.generator.blog

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Path

class BlogGenerator : WebsiteGenerator {
    override val executionPriority: Int = 20_000
    override val generatorName: String = "Reisishot Blog generator"

    override fun isGenerationNeeded(p: Path, extension: String): Boolean = extension.equals("md", true)

    override fun generate(
        filename: List<Path>,
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        //TODO add logic
    }
}