package pictures.reisishot.mise.backend.generator.multisite

import pictures.reisishot.mise.backend.config.*
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.commons.withChild
import java.nio.file.Path

@Suppress("unused")
class ImageInfoImporter constructor(
    private val otherCacheDir: Path,
    private val rootUrl: String
) : WebsiteGenerator {
    override val executionPriority: Int = 25_000
    override val generatorName: String = "ImageInfoImport"

    private fun execute(alreadyRunGenerators: List<WebsiteGenerator>, configuration: WebsiteConfig) =
        configuration.useJsonParser {
            val galleryGenerator =
                alreadyRunGenerators.find { it is AbstractGalleryGenerator } as? AbstractGalleryGenerator
                    ?: throw IllegalStateException("Gallery generator is needed for this generator!")

            (otherCacheDir withChild "gallery.cache.json").fromJson<AbstractGalleryGenerator.Cache>()
                ?.imageInformationData
                ?.forEach { (name, data) ->
                    galleryGenerator.cache.imageInformationData[name] = data.toExternal(rootUrl)
                }
        }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        execute(alreadyRunGenerators, configuration)
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        return false
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfig, cache: BuildingCache) {
        // Nothing to do
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        // Nothing to do
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfig, cache: BuildingCache) {
        // Nothing to do
    }
}
