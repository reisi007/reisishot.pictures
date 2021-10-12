import at.reisishot.mise.commons.filenameWithoutExtension
import at.reisishot.mise.commons.hasExtension
import at.reisishot.mise.commons.toJson
import at.reisishot.mise.commons.withChild
import at.reisishot.mise.config.ImageConfig
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import java.nio.file.Files
import java.nio.file.Paths

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val url = Paths.get(".", "input", "reisishot.pictures", "images").toAbsolutePath().normalize()
    val HOCON = Hocon {
    }

    Files.list(url)
        .filter { it.hasExtension({ it.equals("conf", true) }) }
        .map { it to (it.parent withChild "${it.filenameWithoutExtension}.json") }
        .forEach { (source, target) ->
            val imageConfig = ConfigFactory.parseFile(source.toFile()).extract<ImageConfig>()
            imageConfig.toJson(target)
        }
}
