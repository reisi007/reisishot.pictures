package pictures.reisishot.mise.backend.config.category

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.ConcurrentSet
import at.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.backend.config.ImageInformation

interface CategoryComputable {
    val categoryName: CategoryName
    val defaultImage: FilenameWithoutExtension?
    val images: ConcurrentSet<ImageInformation>
    val subcategories: MutableSet<CategoryComputable>

    fun matchImage(
        imageToProcess: ImageInformation,
        localeProvider: LocaleProvider
    )

    fun toCategoryInformation(): CategoryInformation {
        val categoryInformation = CategoryInformation(
            categoryName,
            images,
            defaultImage?.let { di ->
                images.find { it.filename == di }
                    ?: error("Image $di cannot be found in category $categoryName")
            } ?: images.first(),
            subcategories.asSequence().map { it.toCategoryInformation() }.toSet()
        )

        return categoryInformation
    }

}

internal abstract class NoOpComputable : CategoryComputable {
    override val defaultImage: FilenameWithoutExtension? = null

    override fun matchImage(imageToProcess: ImageInformation, localeProvider: LocaleProvider) {
        error("No implementation needed as DateCategoryComputable does the computation")
    }
}
