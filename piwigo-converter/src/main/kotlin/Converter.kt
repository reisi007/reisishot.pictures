import com.beust.klaxon.Json
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.Jdbi
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Converter {
    private val jsonConverter by lazy { Klaxon() }

    @JvmStatic
    fun main(args: Array<String>) {
        convertPiwigo2Mise(
            "https://reisishot.pictures",
            "jdbc:mariadb://localhost:3306/import?user=root",
            Paths.get("D:\\Reisishot\\MiSe\\backend\\src\\main\\resources\\images").apply {
                Files.createDirectories(this)
            }
        )
    }

    private fun convertPiwigo2Mise(piwigoEndpoint: String, connectionString: String, targetPath: Path) {
        // Get SQL connection
        val jdbi = Jdbi.create(connectionString)

        jdbi.open().use { db ->
            db.createQuery("select id from gal_images")
                .mapTo(Int::class.java)
                .list()
        }.foreachParallel {
            downloadImage(it, piwigoEndpoint, targetPath)
        }
    }

    data class ImageInfoResult(
        val file: String,
        val name: String,
        @Json(name = "element_url") val elementUrl: String,
        val tags: List<String>
    )

    private suspend fun downloadImage(id: Int, piwigoEndpoint: String, baseFolder: Path) {
        val endpoint = URL("${piwigoEndpoint}/ws.php?format=json&method=pwg.images.getInfo&image_id=$id")
        val imageInfo = withContext(Dispatchers.IO) {
            InputStreamReader(endpoint.openStream(), Charsets.UTF_8).use {
                jsonConverter.parseJsonObject(it).let {
                    val jsonResult = it.obj("result")!!
                    ImageInfoResult(
                        jsonResult.string("file")!!,
                        jsonResult.string("name")!!,
                        jsonResult.string("element_url")!!,
                        jsonResult.array<JsonObject>("tags")?.let {
                            it.map { it.string("name")!! }
                        } ?: throw java.lang.IllegalStateException("Image with ID $id has no associated tags")
                    )
                }
            }
        }

        val filenameWithoutExtension = imageInfo.file.filenameWithoutExtension
        val hoconFile = """
            title = ${imageInfo.name}
            url = $filenameWithoutExtension
            tags= [
            %s
            ]
        """.trimIndent().format(imageInfo.tags.joinToString("\n"))

        withContext(Dispatchers.IO)
        {
            val outFile = baseFolder.resolve("$filenameWithoutExtension.jpg")
            URL(imageInfo.elementUrl).openStream().use { inputStream ->
                Files.copy(inputStream, outFile)
            }
            Files.newBufferedWriter(baseFolder.resolve("$filenameWithoutExtension.conf")).use {
                it.write(hoconFile)
            }
        }
    }
}

internal val Path.filenameWithoutExtension: String
    get() = fileName.toString().filenameWithoutExtension
internal val String.filenameWithoutExtension: String
    get() = substring(0, lastIndexOf('.'))


internal fun <T> Iterable<T>.foreachParallel(action: suspend (T) -> Unit) = runBlocking {
    forEach { launch { action(it) } }

}