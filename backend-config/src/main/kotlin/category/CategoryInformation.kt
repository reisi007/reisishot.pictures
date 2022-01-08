package pictures.reisishot.mise.backend.config.category

import at.reisishot.mise.commons.CategoryName
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.config.ImageInformation

@Serializable
data class CategoryInformation(
    val categoryName: CategoryName,
    val images: Set<ImageInformation>,
    val subcategories: Set<CategoryInformation>,
    val visible: Boolean = true

) {
    fun flatten(): Sequence<CategoryInformation> =
        sequenceOf(this) + subcategories.asSequence().flatMap { it.flatten() }

    val urlFragment by lazy {
        categoryName.complexName
            .lowercase()
            .replace(' ', '-')
    }

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
