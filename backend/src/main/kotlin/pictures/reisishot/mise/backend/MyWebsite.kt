package pictures.reisishot.mise.backend

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import java.nio.file.Paths
import java.util.*

@ObsoleteCoroutinesApi
object MyWebsite {

    @JvmStatic
    fun main(args: Array<String>) {
        WebsiteConfiguration(
            "Reisishot - Hobbyfotograf Florian Reisinger",
            Paths.get("../../src/main/resources"),
            generators = arrayOf(
                GalleryGenerator()
            )
        ).build()
    }

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


    fun WebsiteConfiguration.build() {
        println("Start generation of Website...")
        println()
        System.currentTimeMillis().let { startTime ->

            val generatorMap = TreeMap<Int, MutableList<WebsiteGenerator>>()

            generators.forEach { generator ->
                generatorMap.computeIfAbsent(generator.executionPriority) { mutableListOf() } += generator
            }
            println("Started plugin setup")
            println()
            generatorMap.forEachLimitedParallel { it.setup() }
            println()
            println("Finished plugin setup")
            println()
            println("Building website using the following generator configuration... ")
            println()
            generatorMap.forEach { priority, generators ->
                println(
                    "$priority -> " + generators.joinToString(prefix = "[", postfix = "]") { it.generatorName }
                )
                println()
                println("Website generation finished in ${System.currentTimeMillis() - startTime} ms...")
            }
            println()
            println("Started plugin teardown")
            println()
            generatorMap.forEachParallel { it.teardown() }
            println()
            println("Finished plugin teardown")

        }
    }
}