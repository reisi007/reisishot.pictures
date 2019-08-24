import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.asSequence

object PrepareImages {

    @JvmStatic
    fun main(args: Array<String>) {
        val folder = Paths.get(args.first())
        val configFile = """
                        title = 
                        tags = [
                        
                        ]
                    """.trimIndent()

        println("Configs to touch...")
        Files.list(folder).asSequence().filterNotNull()
                .filter { it.toString().let { it.endsWith("jpg", ignoreCase = true) || it.endsWith("jpeg", ignoreCase = true) } }
                .map { it.resolveSibling("${it.filenameWithoutExtension}.conf") }
                .filter { !Files.exists(it) }
                .peek { println(it) }
                .forEach {
                    Files.newBufferedWriter(it).use { it.write(configFile) }
                }

        println("EOP")
    }
}