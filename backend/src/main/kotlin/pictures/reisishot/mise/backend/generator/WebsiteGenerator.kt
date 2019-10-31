package pictures.reisishot.mise.backend.generator

import pictures.reisishot.mise.backend.FileExtension
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.fileExtension
import pictures.reisishot.mise.backend.generator.ChangeState.CREATE
import pictures.reisishot.mise.backend.generator.ChangeState.DELETE
import java.nio.file.Path

interface WebsiteGenerator {
    /**
     * The higher the priority, the sooner the execution.
     * The sitemap plugin, which runs last, has a defined priority of 0
     */
    val executionPriority: Int get() = 10000

    val generatorName: String


    suspend fun fetchInitialInformation(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            alreadyRunGenerators: List<WebsiteGenerator>
    )

    suspend fun fetchUpdateInformation(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            alreadyRunGenerators: List<WebsiteGenerator>,
            changedFiles: ChangedFileset
    ): Boolean

    suspend fun buildInitialArtifacts(
            configuration: WebsiteConfiguration,
            cache: BuildingCache
    )

    suspend fun buildUpdateArtifacts(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            changedFiles: ChangedFileset
    ): Boolean

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

    suspend fun cleanup(
            configuration: WebsiteConfiguration,
            cache: BuildingCache
    )

    fun println(a: Any?) {
        kotlin.io.println("[GENERATOR] [$generatorName] $a")
    }
}

fun FileExtension.isJpeg() = equals("jpg", true) || equals("jpeg", true)

fun FileExtension.isMarkdown() = equals("md", true)

fun FileExtension.isConf() = equals("conf", true)

fun FileExtension.isJson() = equals("json", true)

fun FileExtension.isHtml() = equals("html", true) || equals("htm", true)

fun FileExtension.isJetbrainsTemp() = contains("__jb_")

fun Path.hasExtension(vararg predicates: (FileExtension) -> Boolean) = fileExtension.isAny(*predicates)

fun FileExtension.isAny(vararg predicates: (FileExtension) -> Boolean) = predicates.any { it(this) }

enum class ChangeState {
    CREATE, EDIT, DELETE
}

typealias ChangedFileset = Map<Path, Set<ChangeState>>

fun ChangedFileset.hasDeletions(vararg predicates: (FileExtension) -> Boolean) = asSequence()
        .filter { (_, changedStates) -> changedStates.isStateDeleted() }
        .any { (file, _) -> file.hasExtension(*predicates) }

fun Set<ChangeState>.isStateEdited() = (contains(DELETE) && contains(CREATE)) || contains(ChangeState.EDIT)

fun Set<ChangeState>.isStateDeleted() = contains(DELETE) && !contains(CREATE)