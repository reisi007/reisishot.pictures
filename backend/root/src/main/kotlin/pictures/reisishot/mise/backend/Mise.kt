package pictures.reisishot.mise.backend

import at.reisishot.mise.backend.config.*
import at.reisishot.mise.backend.config.ChangeState.*
import at.reisishot.mise.commons.forEachLimitedParallel
import at.reisishot.mise.commons.forEachParallel
import at.reisishot.mise.commons.hasExtension
import at.reisishot.mise.commons.prettyPrint
import com.sun.nio.file.ExtendedWatchEventModifier
import kotlinx.coroutines.*
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object Mise {

    suspend fun WebsiteConfig.generate() {
        val cache = loadCache()

        setupEnvironment()
        val generators = setupGenerators(cache)

        generateWebsite(cache, generators)

        cache.store(this, generators)

        startIncrementalGeneration(cache, generators)
    }

    private suspend fun WebsiteConfig.setupEnvironment() = doing("Preparing website build") {

        withContext(Dispatchers.IO) {
            Files.createDirectories(paths.targetFolder)
            Files.createDirectories(paths.cacheFolder)
        }

    }

    private suspend fun WebsiteConfig.generateWebsite(
        buildingCache: BuildingCache,
        generatorMap: Map<Int, List<WebsiteGenerator>>
    ) = doing("Building website") {
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

    private fun WebsiteConfig.loadCache(): BuildingCache = BuildingCache().also { it.loadCache(this) }

    private suspend fun WebsiteConfig.setupGenerators(cache: BuildingCache): Map<Int, List<WebsiteGenerator>> =
        doing("Reading / generating cache...") {
            // It is very important that this map is sorted!
            val generatorMap = TreeMap<Int, MutableList<WebsiteGenerator>>()

            generators.forEach { generator ->
                generatorMap.computeIfAbsent(generator.executionPriority) { mutableListOf() } += generator
            }

            generatorMap.values.flatten().forEachParallel { it.loadCache(this@setupGenerators, cache) }


            val runGenerators = mutableListOf<WebsiteGenerator>()
            generatorMap.forEachLimitedParallel {
                it.fetchInitialInformation(this@setupGenerators, cache, runGenerators)
                if (miseConfig.cleanupGeneration)
                    it.cleanup(this, cache)
                runGenerators += it
            }

            generatorMap
        }

    private suspend fun BuildingCache.store(
        websiteConfig: WebsiteConfig,
        generatorMap: Map<Int, List<WebsiteGenerator>>
    ) = doing("Writing cache...") {
        generatorMap.values
            .flatten()
            .forEachParallel { it.saveCache(websiteConfig, this@store) }
        saveCache(websiteConfig)
    }

    private suspend fun WebsiteConfig.startIncrementalGeneration(
        cache: BuildingCache,
        generators: Map<Int, List<WebsiteGenerator>>
    ) = doing("Waiting for changes in order to perform an incremental build") {
        val watchKey = withContext(Dispatchers.IO) {
            FileSystems.getDefault().newWatchService().let {
                paths.sourceFolder.register(
                    it,
                    arrayOf(ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE),
                    ExtendedWatchEventModifier.FILE_TREE
                )
            }
        }
        val coroutineDispatcher = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        ).asCoroutineDispatcher()

        val loopMs = miseConfig.interactiveDelayMs
        while (loopMs != null) {
            delay(loopMs)
            try {
                processEvents(watchKey, this, cache, paths.sourceFolder, generators, coroutineDispatcher)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun processEvents(
        watchKey: WatchKey,
        configuration: WebsiteConfig,
        cache: BuildingCache,
        watchedDir: Path,
        generatorMap: Map<Int, List<WebsiteGenerator>>,
        coroutineDispatcher: CoroutineDispatcher
    ) {
        val changedFileset = watchKey.pollEvents(configuration, watchedDir)
        if (changedFileset.isNotEmpty())
            doing("Incremental build") {
                val generators = mutableListOf<WebsiteGenerator>()
                val changed = AtomicInteger(0)
                generatorMap.forEachLimitedParallel {
                    val cacheChanged =
                        it.fetchUpdateInformation(configuration, cache, generators, changedFileset)
                    if (cacheChanged)
                        changed.addAndGet(1)
                }
                generatorMap.forEachLimitedParallel(coroutineDispatcher) {
                    val cacheChanged = it.buildUpdateArtifacts(configuration, cache, changedFileset)
                    if (cacheChanged)
                        changed.addAndGet(1)
                }
                if (changed.get() > 0)
                    cache.saveCache(configuration)
            }
    }

    private suspend fun WatchKey.pollEvents(configuration: WebsiteConfig, watchedDir: Path): ChangeFileset {
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

    private fun mapToInternal(
        polledEvents: List<WatchEvent<*>>,
        watchedDir: Path,
        configuration: WebsiteConfig
    ): MutableChangedFileset {
        val events: MutableChangedFileset = mutableMapOf()

        polledEvents.forEach { event ->
            val path = event.getContextPath()
                ?.let { watchedDir.resolve(it).normalize() }
                ?.filterIllegalPaths(configuration)
                ?: return@forEach
            val kind = event.kind()?.asChangeState() ?: return@forEach
            events.computeIfAbsent(path) { HashSet() } += kind
        }

        return events
    }

    private fun Path?.filterIllegalPaths(websiteConfig: WebsiteConfig): Path? = this?.let {
        if (
            Files.exists(this) &&
            Files.isRegularFile(this) &&
            !it.hasExtension(websiteConfig.miseConfig.interactiveIgnoredFiles)
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
