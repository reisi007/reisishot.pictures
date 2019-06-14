package pictures.reisishot.mise.backend.generator.gallery

import kotlinx.coroutines.ObsoleteCoroutinesApi
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Path

@ObsoleteCoroutinesApi
class GalleryGenerator(
    private vararg val categoryBuilders: CategoryBuilder,
    private val exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }
) : WebsiteGenerator,
    ImageInformationRepository {

    override val generatorName: String = "Reisishot Gallery"
    override val executionPriority: Int = 20_000

    private val imageInformationCache: Map<Path, InternalImageInformation> = mutableMapOf()
    override val allImageInformationData: Collection<ImageInformation> = imageInformationCache.values

    private val computedTagsInternal: MutableMap<TagName, MutableSet<ImageInformation>> = mutableMapOf()
    override val computedTags: Map<TagName, Set<ImageInformation>> = computedTagsInternal

    override suspend fun generate(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        val thumbnailGenerator = alreadyRunGenerators.find { it is ThumbnailGenerator } as? ThumbnailGenerator
            ?: throw IllegalStateException("Thumbnail generator has bot run yet, however this is needed for this generator")
        //TODO not implemented
    }


    override suspend fun setup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        super.setup(configuration, cache)
        categoryBuilders.forEach { it.setup(configuration, cache) }
    }

    override suspend fun teardown(configuration: WebsiteConfiguration, cache: BuildingCache) {
        super.teardown(configuration, cache)
        categoryBuilders.forEach { it.teardown(configuration, cache) }
    }
}