package pictures.reisishot.mise.backend.generator

import pictures.reisishot.mise.backend.WebsiteConfiguration
import java.nio.file.Path

interface WebsiteGenerator {
    /**
     * The higher the priority, the sooner the execution.
     * The sitemap plugin, which runs last, has a defined priority of 0
     */
    val executionPriority: Int get() = 10000

    val generatorName: String

    /**
    Should return *true* if this plugin needs to regenerate some files
     */
    fun isGenerationNeeded(p: Path, extension: String): Boolean

    fun generate(
        filename: List<Path>,
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    )

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
        kotlin.io.println("[GENERATOR] [$generatorName] $a")
    }
}