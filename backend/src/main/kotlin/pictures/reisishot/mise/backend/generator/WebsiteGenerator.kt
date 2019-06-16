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


    suspend fun fetchInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    )

    suspend fun buildArtifacts(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    )

    suspend fun loadCache(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        println("Load cache")
    }

    suspend fun saveCache(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        println("Save cache")
    }

    fun println(a: Any?) {
        kotlin.io.println("[GENERATOR] [$generatorName] $a")
    }

    val Path.isJpeg
        get() = toString().let { filename ->
            filename.endsWith("jpg", true) || filename.endsWith("jpeg", true)
        }

    val Path.isMarkdown
        get() = toString().endsWith("mk", true)

    val Path.isConf
        get() = toString().endsWith("conf", true)

    val Path.isJson
        get() = toString().endsWith("json", true)
}