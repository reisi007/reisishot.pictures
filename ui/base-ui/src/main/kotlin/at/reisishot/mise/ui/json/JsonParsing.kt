package at.reisishot.mise.ui.json

import at.reisishot.mise.commons.exists
import at.reisishot.mise.commons.isRegularFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

val JSON by lazy {
    Json {
        allowStructuredMapKeys = true
    }
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> T.toJson(path: Path) {
    path.parent?.let {
        Files.createDirectories(it)
        Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            .use { os ->
                JSON.encodeToStream(this, os)
            }
    }
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> Path.fromJson(): T? =
    if (!(exists() && isRegularFile())) null else
        Files.newInputStream(this).use { reader ->
            JSON.decodeFromStream(reader)
        }

