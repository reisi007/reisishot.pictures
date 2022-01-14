package pictures.reisishot.mise.backend.config.category

import at.reisishot.mise.backend.config.LocaleProvider
import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.ConcurrentSet
import at.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.backend.config.ImageInformation

interface CategoryComputable {
    val complexName: String
    val categoryName: CategoryName
    val defaultImage: FilenameWithoutExtension?
    val images: ConcurrentSet<ImageInformation>
    val subcategories: MutableSet<CategoryComputable>
    val visible: Boolean
        get() = true

    fun matchImage(
        imageToProcess: ImageInformation,
        localeProvider: LocaleProvider
    )

}

fun CategoryComputable.toCategoryInformation(): CategoryInformation {
    val mappedSubcategories =
        if (subcategories.size == 1)
            emptySet()
        else
            subcategories.asSequence()
                .map { it.toCategoryInformation() }
                .toSet()

    return CategoryInformation(
        categoryName,
        images,
        defaultImage?.let { di ->
            images.find { it.filename == di }
                ?: error("Image $di cannot be found in category $categoryName")
        } ?: images.last(),
        mappedSubcategories,
        visible
    )
}

internal abstract class NoOpComputable : CategoryComputable {
    override val defaultImage: FilenameWithoutExtension? = null

    override fun matchImage(imageToProcess: ImageInformation, localeProvider: LocaleProvider) {
        error("No implementation needed as DateCategoryComputable does the computation")
    }
}
