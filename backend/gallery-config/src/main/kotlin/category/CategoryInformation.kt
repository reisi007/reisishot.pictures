package pictures.reisishot.mise.backend.config.category

import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.commons.CategoryName
import pictures.reisishot.mise.commons.toUrlsafeString

@Serializable
data class CategoryInformation(
    val categoryName: CategoryName,
    val images: Set<ImageInformation>,
    val thumbnailImage: ImageInformation?,
    val subcategories: Set<CategoryInformation>,
    val visible: Boolean

) {
    fun flatten(): Sequence<CategoryInformation> =
        sequenceOf(this) + subcategories.asSequence().flatMap { it.flatten() }

    val urlFragment by lazy { categoryName.complexName.toUrlsafeString().lowercase() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CategoryInformation

        if (categoryName != other.categoryName) return false

        return true
    }

    override fun hashCode(): Int {
        return categoryName.hashCode()
    }
}

typealias CategoryInformationRoot = Set<CategoryInformation>
