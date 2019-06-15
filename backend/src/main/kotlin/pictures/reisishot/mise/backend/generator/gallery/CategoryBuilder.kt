package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import java.nio.file.Path

interface CategoryBuilder {

    val builderName: String

    suspend fun generateCategories(
        imageInformationRepository: ImageInformationRepository,
        websiteConfiguration: WebsiteConfiguration
    ): Sequence<Pair<FilenameWithoutExtension, CategoryName>>

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

typealias ImageFilename = Path
typealias CategoryName = String
typealias TagName = String
typealias FilenameWithoutExtension = String