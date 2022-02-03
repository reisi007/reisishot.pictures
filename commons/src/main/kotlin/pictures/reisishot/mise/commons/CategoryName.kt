package pictures.reisishot.mise.commons

import kotlinx.serialization.Serializable

@Serializable
class CategoryName(
    val complexName: ComplexName,
    val sortKey: String = complexName,
    val displayName: String = complexName.substringAfterLast("/")
) : Comparable<CategoryName> {
    override fun compareTo(other: CategoryName): Int = compareValuesBy(
        this, other,
        CategoryName::sortKey,
        CategoryName::complexName
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CategoryName

        if (complexName != other.complexName) return false
        if (sortKey != other.sortKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = complexName.hashCode()
        result = 31 * result + sortKey.hashCode()
        return result
    }

    override fun toString() = displayName
}
