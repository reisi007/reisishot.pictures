package pictures.reisishot.mise.backend

import kotlinx.coroutines.*
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Files
import java.util.*

@ObsoleteCoroutinesApi
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
            }

            val generatorMap = TreeMap<Int, MutableList<WebsiteGenerator>>()
            BuildingCache.setup(this)
            generators.forEach { generator ->
                generatorMap.computeIfAbsent(generator.executionPriority) { mutableListOf() } += generator
            }
            println("Started plugin setup")
            println()
            generatorMap.forEachLimitedParallel { it.setup(this, BuildingCache) }
            println()
            println("Finished plugin setup")
            println()
            println("Building website using the following generator configuration... ")
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
                        generator.generate(this@execute, BuildingCache, runGenerators)
                    }
                }
                runGenerators += generators
            }
            println()
            println("Website generation finished in ${System.currentTimeMillis() - startTime} ms...")
            println()
            println("Started plugin teardown")
            println()
            generatorMap.forEachParallel { it.teardown(this, BuildingCache) }
            BuildingCache.teardown(this)
            println()
            println("Finished plugin teardown")
        }
    }
}