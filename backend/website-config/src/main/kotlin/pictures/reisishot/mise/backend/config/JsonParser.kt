package pictures.reisishot.mise.backend.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import pictures.reisishot.mise.commons.isRegularFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists

private const val CONFIG_KEY = "PARSER"

fun <T> WebsiteConfig.useJsonParser(action: JsonParser.() -> T): T {
    val json = additionalConfig[CONFIG_KEY] as JsonParser
    return action(json)
}

suspend fun <T> WebsiteConfig.useJsonParserParallel(action: suspend JsonParser.() -> T): T {
    val json = additionalConfig[CONFIG_KEY] as JsonParser
    return action(json)
}

@WebsiteConfigBuilderDsl
fun WebsiteConfigBuilder.configureJsonParser(action: SerializersModuleBuilder.() -> Unit) {
    additionalConfig[CONFIG_KEY] = JsonParser(action)
}

class JsonParser(action: SerializersModuleBuilder.() -> Unit) {
    val json by lazy {
        Json {
            allowStructuredMapKeys = true
            encodeDefaults = false
            serializersModule = SerializersModule {
                action(this)
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
