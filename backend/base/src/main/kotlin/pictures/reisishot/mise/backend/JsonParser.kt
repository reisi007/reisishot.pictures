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
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import pictures.reisishot.mise.backend.config.ImageInformation as ConfigImageInformation

val WebsiteConfiguration.parser: JsonParser
    get() = extensions.computeIfAbsent("PARSER") { JsonParser(this) } as JsonParser


class JsonParser(websiteConfiguration: WebsiteConfiguration) {

    val json by lazy {
        Json {
            allowStructuredMapKeys = true
            serializersModule = SerializersModule {
                polymorphic(ConfigImageInformation::class) {
                    subclass(InternalImageInformation::class)
                }
                polymorphic(ImageInformation::class) {
                    subclass(InternalImageInformation::class)
                }
                websiteConfiguration.registerSerializer(this)
            }

        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> T.toJson(path: Path) {
        path.parent?.let {
            Files.createDirectories(it)
            Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                .use { os ->
                    json.encodeToStream(this, os)
                }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> Path.fromJson(): T? =
        if (!(exists() && isRegularFile())) null else
            Files.newInputStream(this).use { reader ->
                json.decodeFromStream(reader)
            }
}
