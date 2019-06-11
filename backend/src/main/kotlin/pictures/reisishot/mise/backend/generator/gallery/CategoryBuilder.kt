package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.WebsiteConfiguration

interface CategoryBuilder {

    val builderName: String

    fun generateCategories(
        imageInformationRepository: ImageInformationRepository,
        websiteConfiguration: WebsiteConfiguration
    ): Sequence<CategoryName>

    fun setup() {
        println("Setup")
    }

    fun teardown() {
        println("Teardown")
    }

    fun println(a: Any?) {
        kotlin.io.println("[CATEGORY BUILDER] [$builderName] $a")
    }
}