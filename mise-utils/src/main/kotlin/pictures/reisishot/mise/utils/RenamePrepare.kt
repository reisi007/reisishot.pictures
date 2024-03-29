package pictures.reisishot.mise.utils

import pictures.reisishot.mise.commons.filenameWithoutExtension
import pictures.reisishot.mise.commons.peek
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.TreeMap
import java.util.TreeSet
import kotlin.math.max
import kotlin.streams.asSequence

object RenamePrepare {

    @JvmStatic
    fun main(args: Array<String>) {
        val csvPath = args[0]
        val inputFolder = args[1]
        val namePatterns: MutableMap<String, MutableSet<Int>> = TreeMap()
        val maximumNumber: MutableMap<String, Int> = TreeMap()
        val pattern = Regex("^(.+?)(\\d+)\$")
        PrintStream(Files.newOutputStream(Paths.get(csvPath))).use { writer ->
            Paths.get(inputFolder).let { basePath ->
                Files.walk(basePath).asSequence()
                    .filter { Files.isRegularFile(it) }
                    .sortedBy { Files.getLastModifiedTime(it) }
                    .map { it.filenameWithoutExtension }
                    .distinct()
                    .peek {
                        pattern.matchEntire(it).let { result ->
                            val (filename, count) = result?.destructured
                                ?: throw IllegalStateException("Filename $it is not valid!")
                            namePatterns.computeIfAbsent(filename) { TreeSet() } += count.length
                            maximumNumber.compute(filename) { _, oldVal ->
                                max(count.toInt(), oldVal ?: -1)
                            }
                        }
                    }
                    .map { "$it;$it" }
                    .forEach { writer.println(it) }
            }
        }

        println("The following patterns have been found:")
        namePatterns.forEach { (prefix, countLengths) ->
            print(
                "${
                maximumNumber[prefix].toString().padStart(4, ' ')
                } images with $prefix with count length(s): ${
                countLengths.joinToString {
                    it.toString().padStart(2, ' ')
                }
                }"
            )
            if (countLengths.size == 1)
                println()
            else
                println("\t\t!!!!!!!!!!!!!!!!")
        }
    }
}
