package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache

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
        val complexName: CategoryName,
        val urlFragment: String,
        val visible: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CategoryInformation

        if (complexName != other.complexName) return false

        return true
    }

    override fun hashCode(): Int {
        return complexName.hashCode()
    }
}

fun CategoryConfig.toCategoryInfotmation(visible: Boolean = true) = CategoryInformation(name, url, visible)

val CategoryName.simpleName get() = substringAfterLast("/")

typealias TagName = String
typealias FilenameWithoutExtension = String
typealias CategoryName = String