import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.streams.asSequence

object RenamePrepare {

    @JvmStatic
    fun main(args: Array<String>) {
        val csvPath = args.get(0)
        val inputFolder = args.get(1)
        val namePatterns: MutableMap<String, MutableSet<Int>> = TreeMap()
        val pattern = Regex("^(.+?)(\\d+)\$")
        PrintStream(Files.newOutputStream(Paths.get(csvPath))).use { writer ->
            Paths.get(inputFolder).let { basePath ->
                Files.walk(basePath).asSequence()
                    .filter { Files.isRegularFile(it) }
                    .map { it.filenameWithoutExtension }
                    .distinct()
                    .peek {
                        pattern.matchEntire(it).let { result ->
                            result?.groups?.let { collection ->
                                val pattern =
                                    collection[1]?.value ?: throw IllegalStateException("Filename $it is not valid!")
                                val count =
                                    collection[2]?.value?.length
                                        ?: throw IllegalStateException("Filename $it is not valid!")
                                namePatterns.computeIfAbsent(pattern) { TreeSet() } += count
                            }
                        }
                    }
                    .map { "$it;$it" }
                    .forEach { writer.println(it) }
            }
        }

        println("The following patterns have been found:")
        namePatterns.forEach { prefix, countLengths ->
            print("\t $prefix with count length(s): ${countLengths.joinToString { it.toString() }}")
            if (countLengths.size == 1)
                println()
            else
                println("\t\t!!!!!!!!!!!!!!!!")
        }
    }
}