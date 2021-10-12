package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.CategoryName
import kotlinx.serialization.Serializable

@Serializable
sealed class CategoryInformation {
    abstract val internalName: CategoryName
    abstract val urlFragment: String
    abstract val visible: Boolean

    val complexName
        get() = internalName.complexName

    val displayName
        get() = internalName.displayName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CategoryInformation

        if (this.complexName != other.complexName) return false

        return true
    }

    override fun hashCode(): Int {
        return this.complexName.hashCode()
    }

    abstract fun convert(levelMap: Map<Int, Set<CategoryInformation>>): Set<CategoryName>
}

@Serializable
class DateCategoryInformation(
    override val internalName: CategoryName,
    override val urlFragment: String,
    override val visible: Boolean,
    val isRoot: Boolean
) : CategoryInformation() {
    override fun convert(levelMap: Map<Int, Set<CategoryInformation>>): Set<CategoryName> = if (isRoot) {
        (levelMap[0]?.asSequence() ?: emptySequence())
            .filter { it.displayName.toIntOrNull() != null }
            .map { it.internalName }
            .toSet()
    } else {
        val curCategoryName = internalName
        curCategoryName.complexName.count { it == '/' }.let { categoryLevel ->

            (levelMap[categoryLevel + 1]?.asSequence() ?: emptySequence())
                .filter { it.complexName.startsWith(curCategoryName.complexName) }
                .map { it.internalName }
                .toSet()
        }

    }

}

@Serializable
class DefaultCategoryInformation(
    override val internalName: CategoryName,
    override val urlFragment: String,
    override val visible: Boolean
) : CategoryInformation() {
    override fun convert(levelMap: Map<Int, Set<CategoryInformation>>): Set<CategoryName> {
        val name = complexName
        return name.count { it == '/' }.plus(1)
            .let { subcategoryLevel ->
                (levelMap[subcategoryLevel]?.asSequence() ?: emptySequence())
                    .filter { it.complexName.startsWith(name) }
                    .map { it.internalName }
                    .toSet()
            }
    }
}
