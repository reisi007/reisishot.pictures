package pictures.reisishot.mise.backend

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Files
import java.util.*

@ObsoleteCoroutinesApi
object Mise {

    fun build(configuration: WebsiteConfiguration) = configuration.execute();


    private fun Map<Int, List<WebsiteGenerator>>.forEachLimitedParallel(callable: (WebsiteGenerator) -> Unit) {
        keys.forEach { priority ->
            get(priority)?.let {
                runBlocking {
                    it.forEachLimitedParallel(callable)
                }
            } ?: throw IllegalStateException("No list found for priority $priority")
        }
    }

    private fun Map<Int, List<WebsiteGenerator>>.forEachParallel(callable: (WebsiteGenerator) -> Unit) {
        keys.forEach { priority ->
            get(priority)?.let {
                runBlocking {
                    it.forEachParallel(callable)
                }
            } ?: throw IllegalStateException("No list found for priority $priority")
        }
    }


    private fun WebsiteConfiguration.execute() {
        println("Start generation of Website...")
        println()
        System.currentTimeMillis().let { startTime ->
            Files.createDirectories(outFolder)
            val generatorMap = TreeMap<Int, MutableList<WebsiteGenerator>>()

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
                    generators.forEachLimitedParallel { generator ->
                        //TODO call the individual generators
                    }
                }

            }
            println()
            println("Website generation finished in ${System.currentTimeMillis() - startTime} ms...")
            println()
            println("Started plugin teardown")
            println()
            generatorMap.forEachParallel { it.teardown(this, BuildingCache) }
            println()
            println("Finished plugin teardown")
        }
    }
}