package pictures.reisishot.mise.backend.generator

import pictures.reisishot.mise.backend.FileExtension
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.exists
import pictures.reisishot.mise.backend.fileExtension
import pictures.reisishot.mise.backend.generator.ChangeState.*
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

    /**
     * @return true, if the cache has been changed
     */
    suspend fun fetchUpdateInformation(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            alreadyRunGenerators: List<WebsiteGenerator>,
            changeFiles: ChangeFileset
    ): Boolean

    suspend fun buildInitialArtifacts(
            configuration: WebsiteConfiguration,
            cache: BuildingCache
    )

    /**
     * @return true, if the cache has been changed
     */
    suspend fun buildUpdateArtifacts(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            changeFiles: ChangeFileset
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

fun FileExtension.isTemp() = contains('~')

fun Path.hasExtension(vararg predicates: (FileExtension) -> Boolean) = fileExtension.isAny(*predicates)

fun FileExtension.isAny(vararg predicates: (FileExtension) -> Boolean) = predicates.any { it(this) }

enum class ChangeState {
    CREATE, EDIT, DELETE
}

typealias ChangeFileset = Map<Path, Set<ChangeState>>
typealias MutableChangedFileset = MutableMap<Path, MutableSet<ChangeState>>
typealias ChangeFilesetEntry = Map.Entry<Path, Set<ChangeState>>

fun ChangeFileset.hasDeletions(vararg predicates: (FileExtension) -> Boolean) = asSequence()
        .filter { changedStates -> changedStates.isStateDeleted() }
        .any { (file, _) -> file.hasExtension(*predicates) }

fun ChangeFilesetEntry.isStateEdited() = let { (file, changeSet) ->
    (changeSet.containsAll(listOf(CREATE, DELETE)) || changeSet.contains(EDIT)) && file.exists()
}

fun ChangeFilesetEntry.isStateDeleted() = let { (file, changeSet) ->
    changeSet.contains(DELETE) && !file.exists()
}