package pictures.reisishot.mise.backend.generator.multisite

import at.reisishot.mise.commons.withChild
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.fromXml
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.toExternal
import java.nio.file.Path

class ImageInfoImporter constructor(
    private val otherCacheDir: Path,
    private val rootUrl: String
) : WebsiteGenerator {
    override val executionPriority: Int = 25_000
    override val generatorName: String = "ImageInfoImport"

    private fun execute(alreadyRunGenerators: List<WebsiteGenerator>) {
        val galleryGenerator = alreadyRunGenerators.find { it is AbstractGalleryGenerator } as? AbstractGalleryGenerator
            ?: throw IllegalStateException("Gallery generator is needed for this generator!")

        (otherCacheDir withChild "gallery.cache.xml").fromXml<AbstractGalleryGenerator.Cache>()
            ?.imageInformationData
            ?.forEach { (name, data) ->
                galleryGenerator.cache.imageInformationData[name] = data.toExternal(rootUrl)
            }
    }


    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        execute(alreadyRunGenerators)
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        updateId: Long,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        //TODO enable  execute(alreadyRunGenerators)
        return false
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        updateId: Long,
        changeFiles: ChangeFileset
    ): Boolean {
        // Nothing to do
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }
}
