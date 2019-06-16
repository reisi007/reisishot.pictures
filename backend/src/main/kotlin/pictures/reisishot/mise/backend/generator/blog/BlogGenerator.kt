package pictures.reisishot.mise.backend.generator.blog

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator

class BlogGenerator : WebsiteGenerator {
    override val executionPriority: Int = 20_000
    override val generatorName: String = "Reisishot Blog generator"


    override suspend fun fetchInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        //TODO add logic
    }

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        //TODO add logic
    }
}