package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator

class GalleryGenerator() : WebsiteGenerator {


    override val generatorName: String = "Reisishot Gallery"
    override val executionPriority: Int = 20_000

    override fun isGenerationNeeded(filename: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun generate(filename: String, cache: BuildingCache) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setup() {
        println("Setup")
    }

    override fun teardown() {
        println("Teardown")
    }
}