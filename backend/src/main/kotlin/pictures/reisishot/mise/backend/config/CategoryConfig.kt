package pictures.reisishot.mise.backend.config

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.concurrentSetOf
import at.reisishot.mise.commons.forEachParallel
import kotlinx.coroutines.runBlocking
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation

interface CategoryComputable {
    val categoryName: CategoryName
    val images: MutableSet<InternalImageInformation>
    val subcategories: MutableSet<CategoryComputable>

    fun matchImage(
        imageToProcess: InternalImageInformation,
        websiteConfiguration: WebsiteConfiguration
    )

    fun toCategoryInformation(): CategoryInformation {
        val categoryInformation = CategoryInformation(
            categoryName,
            images,
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

class NewCategoryConfig(
    val name: String,
    var matcher: CategoryMatcher = { false }
) : CategoryComputable {
    override val subcategories: MutableSet<CategoryComputable> = mutableSetOf()
    override val images: MutableSet<InternalImageInformation> = concurrentSetOf()
    override val categoryName by lazy { CategoryName(name) }

    override fun matchImage(
        imageToProcess: InternalImageInformation,
        websiteConfiguration: WebsiteConfiguration
    ) {
        // Depth first
        subcategories.forEach {
            it.matchImage(imageToProcess, websiteConfiguration)
        }

        if (matcher(imageToProcess)) {
            images += imageToProcess
            imageToProcess.categories += categoryName
        }
    }
}

typealias CategoryConfigRoot = MutableSet<CategoryComputable>
typealias  CategoryMatcher = (InternalImageInformation) -> Boolean


fun CategoryConfigRoot.computeCategoryInformation(
    imagesToProcess: List<InternalImageInformation>,
    websiteConfiguration: WebsiteConfiguration
): CategoryInformationRoot {
    runBlocking {
        imagesToProcess.forEachParallel {
            this@computeCategoryInformation.forEach { cc ->
                cc.matchImage(it, websiteConfiguration)
            }
        }
    }
    return toCategoryInformationRoot()
}

