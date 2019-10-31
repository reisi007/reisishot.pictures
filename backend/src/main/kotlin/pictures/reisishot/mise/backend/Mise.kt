package pictures.reisishot.mise.backend

import kotlinx.coroutines.*
import pictures.reisishot.mise.backend.generator.*
import pictures.reisishot.mise.backend.generator.ChangeState.*
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors.*

object Mise {

    fun build(configuration: WebsiteConfiguration) = runBlocking { configuration.execute() }

    private suspend fun WebsiteConfiguration.execute() {
        val cache = loadCache()

        setupEnvironment(cache)
        val generators = setupGenerators(cache)

        generateWebsite(cache, generators)

        cache.store(this, generators)

        startIncrementalGeneration(cache, generators)
    }

    private suspend fun WebsiteConfiguration.setupEnvironment(cache: BuildingCache) = doing("Preparing website build") {
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
        saveCache(websiteConfiguration)

    }

    private suspend fun WebsiteConfiguration.startIncrementalGeneration(cache: BuildingCache, generators: Map<Int, List<WebsiteGenerator>>) = doing("Waiting for changes in order to perform an incremental build") {
        val watchKey = withContext(Dispatchers.IO) {
            FileSystems.getDefault().newWatchService().let {
                inPath.register(it, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
            }
        }
        val coroutineDispatcher = newFixedThreadPoolContext(
                Runtime.getRuntime().availableProcessors(), "Incremental pool"
        )
        while (true) {
            delay(3000)
            watchKey.processEvents(this, cache, generators, coroutineDispatcher)
        }
    }


    private suspend fun WatchKey.processEvents(configuration: WebsiteConfiguration, cache: BuildingCache, generatorMap: Map<Int, List<WebsiteGenerator>>, coroutineDispatcher: CoroutineDispatcher) {
        val polledEvents = pollEvents()
        reset()
        val changedFileset = mapToInternal(polledEvents)
        if (changedFileset.isNotEmpty())
            doing("Incremental build") {
                val generators = mutableListOf<WebsiteGenerator>()
                val changed = AtomicInteger(0)
                generatorMap.forEachLimitedParallel {
                    val cacheChanged = it.fetchUpdateInformation(configuration, cache, generators, changedFileset)
                    if (cacheChanged)
                        changed.addAndGet(1)
                }
                generatorMap.forEachLimitedParallel {
                    it.buildUpdateArtifacts(configuration, cache, changedFileset)
                }
                if (changed.get() > 0)
                    cache.saveCache(configuration)
            }
    }

    fun mapToInternal(polledEvents: List<WatchEvent<*>>): ChangedFileset =
            polledEvents.stream()
                    .map { it.getContextPath() to it.kind() }
                    .map { (path, kind) -> filterIllegalPaths(path) to kind.asChangeState() }
                    .filter { (path, kind) -> kind != null && path != null }
                    .map { (a, b) -> a!! to b!! }
                    .collect(groupingBy({ it.first }, mapping({ it.second }, toSet())))

    private fun filterIllegalPaths(path: Path?): Path? = path?.let {
        if (it.hasExtension(FileExtension::isJetbrainsTemp))
            null
        else it
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