import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.config.ExtImageInformation
import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.LocaleProvider
import pictures.reisishot.mise.backend.config.category.CategoryInformationRoot
import pictures.reisishot.mise.backend.config.category.computeCategoryInformation
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.commons.ComplexName
import pictures.reisishot.mise.commons.ConcurrentSet
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.commons.concurrentSetOf
import pictures.reisishot.mise.commons.concurrentSkipListMap
import pictures.reisishot.mise.commons.fileExtension
import pictures.reisishot.mise.commons.filenameWithoutExtension
import pictures.reisishot.mise.commons.forEachParallel
import pictures.reisishot.mise.commons.isJpeg
import pictures.reisishot.mise.commons.list
import pictures.reisishot.mise.commons.withChild
import pictures.reisishot.mise.config.ImageConfig
import pictures.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.exifdata.readExif
import pictures.reisishot.mise.json.fromJson
import pictures.reisishot.mise.json.toJson
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.ConcurrentMap
import kotlin.io.path.exists

suspend fun Path.computeImagesAndTags() {
    val imageInformation = buildImageInformation()
    val tagInformation = buildTags(imageInformation)
    val categories = buildCategories(imageInformation)

    val tagJson = tagInformation.asSequence().map { (k, v) -> k.url to TagInfo(k.name, v.map { it.filename }) }.toMap()
    val imagesJson = imageInformation.asSequence().map { (key, value) -> key to value.toImageInformation() }.toMap()
    val categoryJson = categories.asSequence()
        .flatMap { it.flatten() }
        .map { category ->
            category.urlFragment to CategoryData(
                category.categoryName.displayName,
                category.images.map { it.filename },
                category.subcategories.map {
                    SubcategoryInformation(
                        it.urlFragment,
                        it.categoryName.displayName,
                        (it.thumbnailImage ?: it.images.first()).filename
                    )
                }
            )
        }.toMap()
    val outPath = resolveSibling("..").withChild("private")
    imagesJson.toJson(outPath.withChild("images.json"))
    tagJson.toJson(outPath.withChild("tags.json"))
    categoryJson.toJson(outPath.withChild("categories.json"))
}

private fun ExtImageInformation.toImageInformation() =
    ImageInformation(filename, categories, tags.asSequence().map { it.url }.toSet())

private suspend fun Path.buildImageInformation(): ConcurrentMap<FilenameWithoutExtension, ExtImageInformation> {
    val imageInformationData = concurrentSkipListMap<FilenameWithoutExtension, ExtImageInformation>()

    this.list()
        .filter { it.fileExtension.isJpeg() }
        .asIterable()
        .forEachParallel { jpegPath ->
            val filenameWithoutExtension = jpegPath.filenameWithoutExtension
            val configPath = jpegPath.parent withChild "$filenameWithoutExtension.json"
            if (!jpegPath.exists())
                throw IllegalStateException("Image path does not exist for $jpegPath!")

            val exif = jpegPath.readExif()

            val imageConfig = configPath.fromJson<ImageConfig>() ?: return@forEachParallel

            val tags = imageConfig.tags
                .map { TagInformation(it) }
                .toCollection(concurrentSetOf())
            val imageInformation = ExtImageInformation(
                filenameWithoutExtension,
                concurrentSetOf(),
                tags, // Needed because single element sets are not correctly loaded as MutableSet
                exif
            )

            imageInformationData[filenameWithoutExtension] = imageInformation
        }
    return imageInformationData
}

private fun buildCategories(imageInformationData: ConcurrentMap<FilenameWithoutExtension, ExtImageInformation>): CategoryInformationRoot {
    val images = imageInformationData.values.toList()

    return PrivateConfig.CATEGORY_CONFIG.computeCategoryInformation(
        images,
        object : LocaleProvider {
            override val locale: Locale
                get() = Locale.getDefault()

        }
    )
}

private suspend fun buildTags(
    imageInformationData: ConcurrentMap<FilenameWithoutExtension, ExtImageInformation>,
): Map<TagInformation, List<ExtImageInformation>> {
    val internalImageInformation = imageInformationData.values.toList()
    PrivateConfig.TAG_CONFIG.computeTags(internalImageInformation)

    val map = concurrentSkipListMap<TagInformation, ConcurrentSet<ExtImageInformation>>()
    internalImageInformation.forEachParallel { imageInfo ->
        imageInfo.tags.forEach {
            val fileSet = map.computeIfAbsent(it) { concurrentSetOf() }
            fileSet.add(imageInfo)
        }

    }

    return map.asSequence()
        .map { (key, value) ->
            key to value.sortedByDescending { it.exifInformation[ExifdataKey.CREATION_DATETIME] }
        }.toMap()


}


@Serializable
data class TagInfo(val name: String, val images: Collection<String>)

@Serializable
data class CategoryData(
    val name: ComplexName,
    val images: List<FilenameWithoutExtension>,
    val subcategories: List<SubcategoryInformation>
)

@Serializable
data class SubcategoryInformation(
    val url: String,
    val name: ComplexName,
    val image: FilenameWithoutExtension,
)
