package pictures.reisinger.next

import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

fun main(args: Array<String>) = runBlocking {
    val path = Paths.get(args.first())

    path.computeMissingExifConfigs()
    path.computeImagesAndTags()
}
