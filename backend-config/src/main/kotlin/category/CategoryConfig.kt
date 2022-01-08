package pictures.reisishot.mise.backend.config.category

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.commons.concurrentSetOf
import at.reisishot.mise.commons.forEachParallel
import kotlinx.coroutines.runBlocking
import pictures.reisishot.mise.backend.config.ImageInformation

interface CategoryComputable {
    val categoryName: CategoryName
    val defaultImage: FilenameWithoutExtension?
    val images: MutableSet<ImageInformation>
    val subcategories: MutableSet<CategoryComputable>

    fun matchImage(
        imageToProcess: ImageInformation,
        localeProvider: LocaleProvider
    )

    fun toCategoryInformation(): CategoryInformation {
        val categoryInformation = CategoryInformation(
            categoryName,
            images,
            defaultImage?.let { di -> images.find { it.filename == di } } ?: images.firstOrNull(),
            subcategories.asSequence().map { it.toCategoryInformation() }.toSet()
        )

        return categoryInformation
    }

}

fun CategoryConfigRoot.toCategoryInformationRoot(): CategoryInformationRoot =
    asSequence()
        .map { it.toCategoryInformation() }
        .toSet()

fun CategoryInformationRoot.flatten(): Sequence<CategoryInformation> = asSequence().flatMap { it.flatten() }

class CategoryConfig(
    val name: String,
    override val defaultImage: FilenameWithoutExtension? = null,
    var matcher: CategoryMatcher = { false }
) : CategoryComputable {
    override val subcategories: MutableSet<CategoryComputable> = mutableSetOf()
    override val images: MutableSet<ImageInformation> = concurrentSetOf()
    override val categoryName by lazy { CategoryName(name) }

    override fun matchImage(
        imageToProcess: ImageInformation,
        localeProvider: LocaleProvider
    ) {
        // Depth first
        subcategories.forEach {
            it.matchImage(imageToProcess, localeProvider)
        }

        if (matcher(imageToProcess)) {
            images += imageToProcess
            imageToProcess.categories += categoryName
        }
    }

    override fun toString(): String {
        return "CategoryConfig(name='$name', subcategories=$subcategories, images=$images)"
    }

}

typealias CategoryConfigRoot = MutableSet<CategoryComputable>
typealias  CategoryMatcher = (ImageInformation) -> Boolean


fun CategoryConfigRoot.computeCategoryInformation(
    imagesToProcess: List<ImageInformation>,
    localeProvider: LocaleProvider
): CategoryInformationRoot {
    runBlocking {
        imagesToProcess.forEachParallel {
            this@computeCategoryInformation.forEach { cc ->
                cc.matchImage(it, localeProvider)
            }
        }
    }
    return toCategoryInformationRoot()
}

