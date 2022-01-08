package pictures.reisishot.mise.backend

import at.reisishot.mise.commons.exists
import at.reisishot.mise.commons.isRegularFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import pictures.reisishot.mise.backend.generator.gallery.ExternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import java.io.StringWriter
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import pictures.reisishot.mise.backend.config.ImageInformation as ConfigImageInformation

private val JSON by lazy {
    Json {
        allowStructuredMapKeys = true
        serializersModule = SerializersModule {
            polymorphic(ConfigImageInformation::class) {
                subclass(InternalImageInformation::class)
            }
            polymorphic(ImageInformation::class) {
                subclass(InternalImageInformation::class)
                subclass(ExternalImageInformation::class)
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T> T.toJson(path: Path) {
    path.parent?.let {
        Files.createDirectories(it)
        Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            .use { os ->
                JSON.encodeToStream(this, os)
            }
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T> Path.fromJson(): T? =
    if (!(exists() && isRegularFile())) null else
        Files.newInputStream(this).use { reader ->
            JSON.decodeFromStream(reader)
        }

internal fun writeToString(action: (Writer) -> Unit) = StringWriter().apply(action).toString()

fun loop(count: Int, action: () -> Unit) {
    for (i in 0 until count)
        action()
}
