package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache

interface CategoryBuilder {

    val builderName: String

    fun generateCategories(
        imageInformationRepository: ImageInformationRepository,
        websiteConfiguration: WebsiteConfiguration
    ): Sequence<Pair<ImageFilename, CategoryName>>

    fun setup(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        println("Setup")
    }

    fun teardown(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        println("Teardown")
    }

    fun println(a: Any?) {
        kotlin.io.println("[CATEGORY BUILDER] [$builderName] $a")
    }
}

typealias ImageFilename = String
typealias CategoryName = String