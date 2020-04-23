package pictures.reisishot.mise.backend

import at.reisishot.mise.commons.forEachLimitedParallel
import at.reisishot.mise.commons.forEachParallel
import at.reisishot.mise.commons.hasExtension
import at.reisishot.mise.commons.prettyPrint
import com.sun.nio.file.ExtendedWatchEventModifier
import kotlinx.coroutines.*
import pictures.reisishot.mise.backend.generator.*
import pictures.reisishot.mise.backend.generator.ChangeState.*
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashSet

object Mise {

    fun build(configuration: WebsiteConfiguration) = runBlocking { configuration.execute() }

    private suspend fun WebsiteConfiguration.execute() {
        val cache = loadCache()

        setupEnvironment()
        val generators = setupGenerators(cache)

        generateWebsite(cache, generators)

        cache.store(this, generators)

        startIncrementalGeneration(cache, generators)
    }

    private suspend fun WebsiteConfiguration.setupEnvironment() = doing("Preparing website build") {
        withContext(Dispatchers.IO) {
            Files.createDirectories(outPath)
            Files.createDirectories(tmpPath)
        }

    }

    private suspend fun WebsiteConfiguration.generateWebsite(buildingCache: BuildingCache, generatorMap: Map<Int, List<WebsiteGenerator>>) = doing("Building website") {
        with(generatorMap) {
            values.asSequence()
                    .flatMap { it.asSequence() }
                    .asIterable()
                    .forEachParallel { it.saveCache(this@generateWebsite, buildingCache) }
        }
        val runGenerators = mutableListOf<WebsiteGenerator>()

        generatorMap.forEachLimitedParallel { generator ->
            generator.buildInitialArtifacts(this@generateWebsite, buildingCache)
            runGenerators += generator
        }
    }

    private fun WebsiteConfiguration.loadCache(): BuildingCache = BuildingCache().also { it.loadCache(this) }

    private suspend fun WebsiteConfiguration.setupGenerators(cache: BuildingCache): Map<Int, List<WebsiteGenerator>> = doing("Reading / generating cache...") {
        // It is very important that this map is sorted!
        val generatorMap = TreeMap<Int, MutableList<WebsiteGenerator>>()

        generators.forEach { generator ->
            generatorMap.computeIfAbsent(generator.executionPriority) { mutableListOf() } += generator
        }

        generatorMap.values.flatten().forEachParallel { it.loadCache(this@setupGenerators, cache) }


        val runGenerators = mutableListOf<WebsiteGenerator>()
        generatorMap.forEachLimitedParallel {
            it.fetchInitialInformation(this@setupGenerators, cache, runGenerators)
            if (cleanupGeneration)
                it.cleanup(this, cache)
            runGenerators += it
        }

        generatorMap
    }

    private suspend fun BuildingCache.store(websiteConfiguration: WebsiteConfiguration, generatorMap: Map<Int, List<WebsiteGenerator>>) = doing("Writing cache...") {
        generatorMap.values.flatMap { it }.forEachParallel { it.saveCache(websiteConfiguration, this@store) }
        saveCache()

    }

    private suspend fun WebsiteConfiguration.startIncrementalGeneration(cache: BuildingCache, generators: Map<Int, List<WebsiteGenerator>>) = doing("Waiting for changes in order to perform an incremental build") {
        val watchKey = withContext(Dispatchers.IO) {
            FileSystems.getDefault().newWatchService().let {
                inPath.register(it, arrayOf(ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE), ExtendedWatchEventModifier.FILE_TREE)
            }
        }
        val coroutineDispatcher = newFixedThreadPoolContext(
                Runtime.getRuntime().availableProcessors(), "Incremental pool"
        )
        while (interactiveDelayMs != null) {
            delay(interactiveDelayMs)
            try {
                watchKey.processEvents(this, inPath, cache, generators, coroutineDispatcher)
            } catch (e: Exception) {
                System.err.println(e.javaClass.canonicalName + ": " + e.message)
            }
        }
    }

    private suspend fun WatchKey.processEvents(configuration: WebsiteConfiguration, watchedDir: Path, cache: BuildingCache, generatorMap: Map<Int, List<WebsiteGenerator>>, coroutineDispatcher: CoroutineDispatcher) {
        val changedFileset = pollEvents(configuration, watchedDir)
        if (changedFileset.isNotEmpty())
            doing("Incremental build") {
                val generators = mutableListOf<WebsiteGenerator>()
                val changed = AtomicInteger(0)
                generatorMap.forEachLimitedParallel {
                    val cacheChanged = it.fetchUpdateInformation(configuration, cache, generators, changedFileset)
                    if (cacheChanged)
                        changed.addAndGet(1)
                }
                generatorMap.forEachLimitedParallel(coroutineDispatcher) {
                    val cacheChanged = it.buildUpdateArtifacts(configuration, cache, changedFileset)
                    if (cacheChanged)
                        changed.addAndGet(1)
                }
                if (changed.get() > 0)
                    cache.saveCache()
            }
    }

    private suspend fun WatchKey.pollEvents(configuration: WebsiteConfiguration, watchedDir: Path): ChangeFileset {
        val polledEvents = pollEvents()
        reset()
        val changedFileset = mapToInternal(polledEvents, watchedDir, configuration)
        changedFileset.prettyPrint()
        if (changedFileset.isNotEmpty()) {
            delay(1000)
            val nextEvents = pollEvents(configuration, watchedDir)
            nextEvents.forEach { (p, changeStates) ->
                changedFileset.computeIfAbsent(p) { mutableSetOf() } += changeStates
            }
        }
        return changedFileset
    }

    fun mapToInternal(polledEvents: List<WatchEvent<*>>, watchedDir: Path, configuration: WebsiteConfiguration): MutableChangedFileset {
        val events: MutableChangedFileset = mutableMapOf()

        polledEvents.forEach {
            val path = it.getContextPath()
                    ?.let { watchedDir.resolve(it).normalize() }
                    ?.filterIllegalPaths(configuration)
                    ?: return@forEach
            val kind = it.kind()?.asChangeState() ?: return@forEach
            events.computeIfAbsent(path) { HashSet() } += kind
        }

        return events
    }

    private fun Path?.filterIllegalPaths(websiteConfiguration: WebsiteConfiguration): Path? = this?.let {
        if (
                Files.exists(this) &&
                Files.isRegularFile(this) &&
                !it.hasExtension(*websiteConfiguration.interactiveIgnoredFiles)
        )
            it
        else null
    }

    private fun WatchEvent.Kind<*>.asChangeState(): ChangeState? = when (this) {
        ENTRY_CREATE -> CREATE
        ENTRY_DELETE -> DELETE
        ENTRY_MODIFY -> EDIT
        else -> null
    }

    private fun WatchEvent<*>.getContextPath() = context() as? Path

    private suspend fun <T> doing(actionName: String, action: suspend () -> T): T {
        printBig("[START] $actionName")

        val result = action()

        printBig("[END]  $actionName")
        return result
    }

    private fun printBig(text: String) {
        println()
        println(text)
        println()
    }
}