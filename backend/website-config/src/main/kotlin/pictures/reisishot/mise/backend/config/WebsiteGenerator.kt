package pictures.reisishot.mise.backend.config

import pictures.reisishot.mise.backend.config.ChangeState.*
import pictures.reisishot.mise.commons.FileExtension
import pictures.reisishot.mise.commons.hasExtension
import java.nio.file.Path
import kotlin.io.path.exists

interface WebsiteGenerator {
    /**
     * The higher the priority, the sooner the execution.
     * The sitemap plugin, which runs last, has a defined priority of 0
     */
    val executionPriority: Int get() = 10000

    val generatorName: String


    suspend fun fetchInitialInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    )

    /**
     * @return true, if the cache has been changed
     */
    suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean

    suspend fun buildInitialArtifacts(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache
    )

    /**
     * @return true, if the cache has been changed
     */
    suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean

    suspend fun loadCache(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache
    ) {
        println("Load cache")
    }

    suspend fun saveCache(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache
    ) {
        println("Save cache")
    }

    suspend fun cleanup(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache
    )

    fun println(a: Any?) {
        kotlin.io.println("[GENERATOR] [$generatorName] $a")
    }
}

enum class ChangeState {
    CREATE, EDIT, DELETE
}

typealias ChangeFileset = Map<Path, Set<ChangeState>>
typealias MutableChangedFileset = MutableMap<Path, MutableSet<ChangeState>>
typealias ChangeFilesetEntry = Pair<Path, Set<ChangeState>>

fun ChangeFileset.hasDeletions(vararg predicates: (FileExtension) -> Boolean) = asSequence()
    .map { (k, v) -> k to v }
    .filter { changedStates -> changedStates.isStateDeleted() }
    .any { (file, _) -> file.hasExtension(*predicates) }

fun ChangeFilesetEntry.isStateEdited() = let { (file, changeSet) ->
    (changeSet.containsAll(listOf(CREATE, DELETE)) || changeSet.contains(EDIT)) && file.exists()
}

fun ChangeFilesetEntry.isStateDeleted() = let { (file, changeSet) ->
    changeSet.contains(DELETE) && !file.exists()
}
