package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.JsonParser
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.category.CategoryConfigRoot
import pictures.reisishot.mise.backend.config.category.CategoryInformation
import pictures.reisishot.mise.backend.config.category.buildCategoryConfig
import pictures.reisishot.mise.backend.config.tags.TagConfig
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.backend.config.tags.buildTagConfig
import pictures.reisishot.mise.backend.config.useJsonParser
import pictures.reisishot.mise.backend.generator.multisite.ExternalImageInformation
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator
import pictures.reisishot.mise.commons.withChild
import pictures.reisishot.mise.config.ImageConfig
import pictures.reisishot.mise.exifdata.ExifdataKey
import java.nio.file.Path
import kotlin.io.path.createDirectories

class JsonGalleryGenerator(
    tagConfig: TagConfig = buildTagConfig { },
    categoryConfig: CategoryConfigRoot = buildCategoryConfig { },
    displayedMenuItems: Set<DisplayedMenuItems> = setOf(DisplayedMenuItems.CATEGORIES, DisplayedMenuItems.TAGS),
    exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it },
    pageGenerationSettings: Set<PageGenerationSettings> = setOf(*PageGenerationSettings.values()),
    imageConfigNotFoundAction: (notFoundConfigPath: Path) -> ImageConfig = { inPath ->
        throw IllegalStateException(
            "Could not load config file $inPath. Please check if the format is valid!"
        )
    }
) : AbstractGalleryGenerator(
    tagConfig,
    categoryConfig,
    displayedMenuItems,
    pageGenerationSettings,
    exifReplaceFunction,
    imageConfigNotFoundAction
) {
    override fun generateImagePage(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        curImageInformation: ImageInformation
    ) {
        val outFolderBase = configuration.paths.targetFolder withChild "gallery/images"
        outFolderBase.createDirectories()
        val targetFile = outFolderBase withChild "${curImageInformation.filename.lowercase()}.json"
        val baseUrl = if (curImageInformation is ExternalImageInformation) curImageInformation.host else null
        configuration.useJsonParser {
            JsonImage(baseUrl, curImageInformation.thumbnailSizes).toJson(targetFile)
        }

    }

    override fun generateCategoryPage(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        categoryInformation: CategoryInformation
    ) {
        val folder = "gallery/categories"
        val outFolderBase = configuration.paths.targetFolder withChild folder withChild categoryInformation.urlFragment
        outFolderBase.createDirectories()

        val images = asJsonImageInformation(categoryInformation.images)

        val baseUrl =
            "${configuration.websiteInformation.normalizedWebsiteLocation}$folder/${categoryInformation.urlFragment}"

        configuration.useJsonParser {
            JsonGalleryFirst(
                categoryInformation.subcategories.map {
                    val thumb = cache.imageInformationData.getValue(it.thumbnailImage.filename)
                        .thumbnailSizes.getValue(AbstractThumbnailGenerator.ImageSize.LARGEST)
                    Subcategory(it.categoryName, it.thumbnailImage.filename, thumb.width, thumb.height)
                },
                images.first(),
                if (images.size == 1) null else "$baseUrl/2.json"
            ).toJson(outFolderBase withChild "1.json")

            buildOtherPages(images, baseUrl, outFolderBase)
        }
    }

    private fun JsonParser.buildOtherPages(
        images: List<List<JsonImageData>>,
        baseUrl: String,
        outFolderBase: Path
    ) {
        for (i in 1 until images.size) {
            val idx = i + 1
            val curImages = images[i]
            JsonGallery(
                curImages,
                if (idx == images.size) null else "$baseUrl/${idx + 1}.json"
            ).toJson(outFolderBase withChild "$idx.json")
        }
    }

    override fun generateTagPage(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        tagInformation: TagInformation
    ) {
        val folder = "gallery/tags"
        val outFolderBase = configuration.paths.targetFolder withChild folder withChild tagInformation.urlFragment
        outFolderBase.createDirectories()

        val images = cache.computedTags.getValue(tagInformation).asSequence().asJsonImageInformation()

        val baseUrl =
            "${configuration.websiteInformation.normalizedWebsiteLocation}$folder/${tagInformation.urlFragment}"

        configuration.useJsonParser {
            JsonGalleryFirst(
                null,
                images.first(),
                if (images.size == 1) null else "$baseUrl/2.json"
            ).toJson(outFolderBase withChild "1.json")

            buildOtherPages(images, baseUrl, outFolderBase)
        }
    }

    override val generatorName: String = "JSON Gallery Generator"

    private fun asJsonImageInformation(images: Collection<pictures.reisishot.mise.backend.config.ImageInformation>): List<List<JsonImageData>> {
        val map = images
            .asSequence()
            .map { cache.imageInformationData.getValue(it.filename) }
        return map.asJsonImageInformation()
    }

    private fun Sequence<ImageInformation>.asJsonImageInformation(): List<List<JsonImageData>> {
        return map { it as? InternalImageInformation }
            .filterNotNull()
            .sortedByDescending { it.exifInformation[ExifdataKey.CREATION_DATETIME] }
            .map {
                val ii = it.thumbnailSizes.getValue(AbstractThumbnailGenerator.ImageSize.LARGEST)
                JsonImageData(it.filename, ii.width, ii.height)
            }
            .windowed(100, 100, true)
            .toList()
    }

}
