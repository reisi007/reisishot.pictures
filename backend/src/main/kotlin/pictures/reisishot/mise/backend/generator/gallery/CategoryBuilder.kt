package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.toFriendlyPathName

interface CategoryBuilder {

    val builderName: String

    suspend fun generateCategories(
            imageInformationRepository: ImageInformationRepository,
            websiteConfiguration: WebsiteConfiguration
    ): Sequence<Pair<FilenameWithoutExtension, CategoryInformation>>

    suspend fun setup(
            configuration: WebsiteConfiguration,
            cache: BuildingCache
    ) {
        println("Setup")
    }

    suspend fun teardown(
            configuration: WebsiteConfiguration,
            cache: BuildingCache
    ) {
        println("Teardown")
    }

    fun println(a: Any?) {
        kotlin.io.println("[CATEGORY BUILDER] [$builderName] $a")
    }
}

data class CategoryInformation(
        val internalName: CategoryName,
        val urlFragment: String,
        val visible: Boolean = true
) {
    val name
        get() = internalName.complexName

    val simpleName
        get() = internalName.simpleName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CategoryInformation

        if (this.name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return this.name.hashCode()
    }
}

fun CategoryConfig.toCategoryInfotmation(visible: Boolean = true) = CategoryInformation(CategoryName(name), name.toFriendlyPathName(), visible)

class TagInformation(val name: String) : Comparator<TagInformation> {
    val url by lazy { name.toFriendlyPathName() }

    override fun compare(o1: TagInformation?, o2: TagInformation?): Int = o1!!.name.compareTo(o2!!.name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagInformation

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "TagName(name='$name')"
    }
}
typealias FilenameWithoutExtension = String