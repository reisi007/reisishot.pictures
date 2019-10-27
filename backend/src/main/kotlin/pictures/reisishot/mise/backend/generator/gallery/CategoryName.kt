package pictures.reisishot.mise.backend.generator.gallery

class CategoryName(val complexName: ComplexName,
                   val sortKey: String = complexName,
                   val displayName: String = complexName.substringAfterLast("/")
) : Comparable<CategoryName> {
    override fun compareTo(other: CategoryName): Int = compareValuesBy(this, other,
            CategoryName::sortKey,
            CategoryName::displayName,
            CategoryName::complexName
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CategoryName

        if (complexName != other.complexName) return false
        if (displayName != other.displayName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = complexName.hashCode()
        result = 31 * result + displayName.hashCode()
        return result
    }

    override fun toString() = displayName
}

typealias ComplexName = String