package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Path

class GalleryGenerator(
    private vararg val categoryBuilders: CategoryBuilder,
    private val exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }
) : WebsiteGenerator,
    ImageInformationRepository {

    override val generatorName: String = "Reisishot Gallery"
    override val executionPriority: Int = 20_000

    private val imageInformationCache: Map<Path, InternalImageInformation> = mutableMapOf()
    override val allImageInformationData: Collection<ImageInformation> = imageInformationCache.values


    override fun isGenerationNeeded(p: Path, extension: String): Boolean = false

    override fun generate(
        filename: List<Path>,
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        val thumbnailGenerator = alreadyRunGenerators.find { it is ThumbnailGenerator } as? ThumbnailGenerator
            ?: throw IllegalStateException("Thumbnail generator has bot run yet, however this is needed for this generator")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getImageInformation(path: Path): ImageInformation {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}