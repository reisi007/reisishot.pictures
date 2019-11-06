package pictures.reisishot.mise.backend


import at.reisishot.mise.commons.exists
import at.reisishot.mise.commons.isRegularFile
import com.drew.imaging.ImageMetadataReader
import com.thoughtworks.xstream.XStream
import pictures.reisishot.mise.backend.generator.gallery.ExifInformation
import pictures.reisishot.mise.backend.generator.gallery.ExifdataKey
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

internal val xStrem by lazy { XStream() }

internal inline fun <reified T> T.toXml(path: Path) {
    path.parent?.let {
        Files.createDirectories(it)
        Files.newBufferedWriter(path, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                .use { writer ->
                    xStrem.toXML(this, writer)
                }
    }
}

internal inline fun <reified T> Path.fromXml(): T? =
        if (!(exists() && isRegularFile())) null else
            Files.newBufferedReader(this, Charsets.UTF_8).use { reader ->
                xStrem.fromXML(reader) as? T
            }

internal fun Path.readExif(exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }): Map<ExifdataKey, String> = mutableMapOf<ExifdataKey, String>().apply {
    ExifInformation(ImageMetadataReader.readMetadata(this@readExif.toFile()))
            .let { exifInformation ->
                ExifdataKey.values().forEach { key ->
                    val exifValue = key.getValue(exifInformation)
                    exifReplaceFunction(key to exifValue)
                            .also { (key, possibleValue) ->
                                if (possibleValue != null)
                                    put(key, possibleValue)
                            }
                }
            }
}