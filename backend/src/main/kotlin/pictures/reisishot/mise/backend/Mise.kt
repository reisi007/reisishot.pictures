package pictures.reisishot.mise.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Files
import java.util.*

object Mise {

    fun build(configuration: WebsiteConfiguration) = runBlocking { configuration.execute() }


    private suspend fun Map<Int, List<WebsiteGenerator>>.forEachLimitedParallel(callable: suspend (WebsiteGenerator) -> Unit) {
        keys.forEach { priority ->
            get(priority)?.let { generators ->
                coroutineScope {
                    generators.forEachLimitedParallel(generators.size) { callable(it) }
                }
            } ?: throw IllegalStateException("No list found for priority $priority")
        }
    }

    private suspend fun Map<Int, List<WebsiteGenerator>>.forEachParallel(callable: suspend (WebsiteGenerator) -> Unit) {
        keys.forEach { priority ->
            get(priority)?.let {
                coroutineScope {
                    it.forEachParallel { callable(it) }
                }
            } ?: throw IllegalStateException("No list found for priority $priority")
        }
    }

    private suspend fun WebsiteConfiguration.execute() {
        println("Start generation of Website...")
        println()
        System.currentTimeMillis().let { startTime ->
            withContext(Dispatchers.IO) {
                Files.createDirectories(outPath)
                Files.createDirectories(tmpPath)
            }

            val generatorMap = TreeMap<Int, MutableList<WebsiteGenerator>>()
            BuildingCache.loadCache(this)
            generators.forEach { generator ->
                generatorMap.computeIfAbsent(generator.executionPriority) { mutableListOf() } += generator
            }
            println("Reading / generating cache...")
            println()
            with(generatorMap.values) {
                flatMap { it }.forEachLimitedParallel(size) { it.loadCache(this@execute, BuildingCache) }
            }
            println()
            println("Reading / generating cache...")
            println()
            println("Preparing website build using the following generator configuration... ")
            println()
            val runGenerators = mutableListOf<WebsiteGenerator>()
            generatorMap.forEach { priority, generators ->
                println(
                    "Executing priority $priority (the following generators " + generators.joinToString(
                        prefix = "[",
                        postfix = "]"
                    ) { it.generatorName } + ")"
                )
                runBlocking {
                    generators.forEachLimitedParallel(generators.size) { generator ->
                        generator.fetchInformation(this@execute, BuildingCache, runGenerators)
                    }
                }
                runGenerators += generators
            }
            println()
            println("Writing cache...")
            println()
            println()
            println("Building website using the following generator configuration... ")
            println()
            with(generatorMap.values) {
                flatMap { it }.forEachLimitedParallel(size) { it.saveCache(this@execute, BuildingCache) }
            }
            generatorMap.forEach { priority, generators ->
                println(
                    "Executing priority $priority (the following generators " + generators.joinToString(
                        prefix = "[",
                        postfix = "]"
                    ) { it.generatorName } + ")"
                )
                runBlocking {
                    generators.forEachLimitedParallel(generators.size) { generator ->
                        generator.buildArtifacts(this@execute, BuildingCache)
                    }
                }
                runGenerators += generators
            }
            println()
            println("Website generation finished in ${System.currentTimeMillis() - startTime} ms...")
            println()
            println("Writing cache...")
            println()
            with(generatorMap.values) {
                flatMap { it }.forEachLimitedParallel(size) { it.saveCache(this@execute, BuildingCache) }
            }
            BuildingCache.saveCache(this)
            println()
            println("Writing cache...")
            println()
            println()
            println("Done!")
        }
    }
}