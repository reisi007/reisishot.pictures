package pictures.reisinger.next

import com.drew.metadata.jpeg.JpegDirectory
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.commons.fileExtension
import pictures.reisishot.mise.commons.fileModifiedDateTime
import pictures.reisishot.mise.commons.forEachParallel
import pictures.reisishot.mise.commons.isJpeg
import pictures.reisishot.mise.commons.isRegularFile
import pictures.reisishot.mise.commons.list
import pictures.reisishot.mise.commons.withNewExtension
import pictures.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.exifdata.readExif
import pictures.reisishot.mise.json.toJson
import java.nio.file.Path
import kotlin.io.path.exists

@Serializable
private data class ExifJson(val exif: Map<ExifdataKey, String>, val width: Int, val height: Int)

val pair = 0 to 0


suspend fun Path.computeMissingExifConfigs() {
    list()
        .filter { it.isRegularFile() && it.fileExtension.isJpeg() }
        .map { it to it.withNewExtension("exif.json") }
        .filter { (image, exifConfig) -> !exifConfig.exists() || exifConfig.fileModifiedDateTime < image.fileModifiedDateTime }
        .forEachParallel { (image, exifConfig) ->
            var wh: Pair<Int, Int> = pair
            val exif = image.readExif {
                val w = jpegDescriptor
                    ?.getDescription(JpegDirectory.TAG_IMAGE_WIDTH)
                    ?.substringBefore(" ")
                    ?.toIntOrNull()
                    ?: throw IllegalStateException("Image width")
                val h = jpegDescriptor?.getDescription(JpegDirectory.TAG_IMAGE_HEIGHT)
                    ?.substringBefore(" ")
                    ?.toIntOrNull()
                    ?: throw IllegalStateException("Image height")
                wh = w to h
            }
            ExifJson(exif, wh.first, wh.second).toJson(exifConfig)
        }
}
