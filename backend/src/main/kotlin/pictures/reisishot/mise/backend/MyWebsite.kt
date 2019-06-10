package pictures.reisishot.mise.backend

import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import java.nio.file.Paths

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


    fun WebsiteConfiguration.build() {
        println("Start generation of Website...")
        System.currentTimeMillis().let { startTime ->

            val generatorMap = mutableMapOf<Int, MutableList<WebsiteGenerator>>()

            generators.forEach { generator ->
                generatorMap.computeIfAbsent(generator.executionPriority) { mutableListOf() } += generator
            }
            println()
            println("Building website using the following generator configuration... ")
            generatorMap.forEach { priority, generators ->
                println(
                    "$priority -> " + generators.joinToString(prefix = "[", postfix = "]") { it.generatorName }
                )
                println()
                println("Website generation finished in ${System.currentTimeMillis() - startTime} ms...")

            }


        }
    }
}