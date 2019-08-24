import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

object RenameDo {

    @JvmStatic
    fun main(args: Array<String>) {
        val csvPath = Paths.get(args.get(0))
        val inputFolder = Paths.get(args.get(1))

        val targetFilenames = mutableSetOf<String>()

        Files.newBufferedReader(csvPath, Charsets.UTF_8).useLines {
            it.map { it.split(';', limit = 2) }
                    .map { it[0] to it[1] }
                    .peek { (_, targetFilename) ->
                        if (!targetFilenames.add(targetFilename))
                            throw IllegalStateException("Cannot perform rename -> $targetFilename is duplicated")
                    }.filter { it.first != it.second }
                    .toList().asSequence()
                    .flatMap { (sourceName, targetName) ->
                        sequenceOf(
                                prepareRename(inputFolder, sourceName, targetName, "jpg"),
                                prepareRename(inputFolder, sourceName, targetName, "conf")
                        )
                    }.forEach { (from, to) ->
                        Files.move(from, to)
                    }
        }
    }

    private fun prepareRename(
            inputFolder: Path,
            sourceFilename: String,
            targetFilename: String,
            extension: String
    ): Pair<Path, Path> {
        val sourcePath = inputFolder.resolve("$sourceFilename.$extension")
        val targetPath = inputFolder.resolve("$targetFilename.$extension")
        val tmpPath = inputFolder.resolve(UUID.randomUUID().toString())

        Files.move(sourcePath, tmpPath)
        return tmpPath to targetPath
    }
}