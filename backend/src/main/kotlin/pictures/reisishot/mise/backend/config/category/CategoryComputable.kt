package pictures.reisishot.mise.backend.config.category

import pictures.reisishot.mise.backend.config.ExtImageInformation
import pictures.reisishot.mise.backend.config.LocaleProvider
import pictures.reisishot.mise.commons.CategoryName
import pictures.reisishot.mise.commons.ConcurrentSet
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.exifdata.ExifdataKey

interface CategoryComputable {
    val complexName: String
    val categoryName: CategoryName
    val defaultImage: FilenameWithoutExtension?
    val images: ConcurrentSet<ExtImageInformation>
    val subcategories: MutableSet<CategoryComputable>

    fun matchImage(
        imageToProcess: ExtImageInformation,
        localeProvider: LocaleProvider
    )
}

fun CategoryComputable.toCategoryInformation(): CategoryInformation {
    val mappedSubcategories =
        if (subcategories.size == 1)
            emptyList()
        else
            subcategories.asSequence()
                .map { it.toCategoryInformation() }
                .sortedBy { it.categoryName.sortKey }
                .toList()

    return CategoryInformation(
        categoryName,
        images.sortedByDescending { it.exifInformation[ExifdataKey.CREATION_DATETIME] },
        defaultImage?.let { di ->
            images.find { it.filename == di }
                ?: error("Image $di cannot be found in category $categoryName")
        } ?: images.lastOrNull() ?: throw IllegalStateException("$categoryName is empty"),
        mappedSubcategories
    )
}

internal abstract class NoOpComputable : CategoryComputable {
    override val defaultImage: FilenameWithoutExtension? = null

    override fun matchImage(imageToProcess: ExtImageInformation, localeProvider: LocaleProvider) {
        error("No implementation needed as DateCategoryComputable does the computation")
    }
}
