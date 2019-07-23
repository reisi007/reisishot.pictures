package pictures.reisishot.mise.backend

import com.thoughtworks.xstream.XStream
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import io.github.config4k.extract
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import pictures.reisishot.mise.backend.generator.gallery.FilenameWithoutExtension
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.swing.ImageIcon
import kotlin.streams.asSequence

suspend fun <E> Iterable<E>.forEachLimitedParallel(
    maxThreadCount: Int, callable: suspend (E) -> Unit
) = forEachParallel(
    newFixedThreadPoolContext(
        Math.min(maxThreadCount, 2 * Runtime.getRuntime().availableProcessors()),
        "Foreach"
    ), callable
)

suspend fun <E> Iterable<E>.forEachParallel(
    dispatcher: CoroutineDispatcher = newFixedThreadPoolContext(
        2 * Runtime.getRuntime().availableProcessors(),
        "Foreach"
    ), callable: suspend (E) -> Unit
) = coroutineScope {
    map { launch(dispatcher) { callable(it) } }
}

infix fun Path.withChild(fileOrFolder: String): Path = resolve(fileOrFolder)
infix fun Path.withChild(fileOrFolder: Path): Path = resolve(fileOrFolder)

fun <T> Path.useInputstream(vararg openOptions: OpenOption, callable: (InputStream) -> T): T =
    Files.newInputStream(this, *openOptions).use(callable)

fun <T> Path.useBufferedReader(callable: (BufferedReader) -> T): T =
    Files.newBufferedReader(this, Charsets.UTF_8).use(callable)

fun Path.getConfig(configParseOptions: ConfigParseOptions = ConfigParseOptions.defaults()): Config? =
    if (exists())
        useBufferedReader { ConfigFactory.parseReader(it, configParseOptions) }
    else null

inline fun <reified T> Path.parseConfig(
    configPath: String,
    fallbackConfiguration: Config,
    configParseOptions: ConfigParseOptions = ConfigParseOptions.defaults()
): T? = getConfig(configParseOptions)?.apply { withFallback(fallbackConfiguration) }?.extract(configPath)


inline fun <reified T> Path.parseConfig(
    configPath: String,
    vararg fallbackPaths: String,
    configParseOptions: ConfigParseOptions = ConfigParseOptions.defaults()
): T? = getConfig(configParseOptions)?.apply {
    var lastConfig = this
    fallbackPaths.forEach {
        getConfig(configParseOptions)?.let { curConfig ->

            lastConfig.withFallback(curConfig)
            lastConfig = curConfig
        }
    }
}?.extract(configPath)


inline fun <reified T> Path.parseConfig(
    configParseOptions: ConfigParseOptions = ConfigParseOptions.defaults()
): T? = getConfig(configParseOptions)?.extract()

fun String.writeTo(p: Path) =
    Files.newBufferedWriter(p, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use {
        it.write(this)
    }

fun Path.readImage(): BufferedImage =
    ImageIcon(toUri().toURL()).let {
        BufferedImage(it.iconWidth, it.iconHeight, BufferedImage.TYPE_INT_RGB).apply {
            it.paintIcon(null, createGraphics(), 0, 0)
        }
    }

val Path.fileModifiedDateTime: ZonedDateTime?
    get() = if (Files.exists(this) && Files.isRegularFile(this))
        Files.getLastModifiedTime(this).toInstant().atZone(ZoneId.systemDefault())
    else null

val Path.filenameWithoutExtension: FilenameWithoutExtension
    get() = with(fileName.toString()) {
        substring(0, lastIndexOf('.'))
    }

val Path.filenameExtension: String
    get() = with(fileName.toString()) {
        substring(lastIndexOf('.') + 1)
    }

fun Path.list(): Sequence<Path> = Files.list(this).asSequence()

fun Path.exists() = Files.exists(this)

fun Path.isRegularFile() = Files.isRegularFile(this)

val xStrem by lazy { XStream() }

inline fun <reified T> T.toXml(path: Path) {
    path.parent?.let {
        Files.createDirectories(it)
        Files.newBufferedWriter(path, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            .use { writer ->
                xStrem.toXML(this, writer)
            }
    }
}

inline fun <reified T> Path.fromXml(): T? =
    if (!(exists() && isRegularFile())) null else
        Files.newBufferedReader(this, Charsets.UTF_8).use { reader ->
            xStrem.fromXML(reader) as? T
        }

inline fun <reified T> Sequence<T>.toArray(size: Int): Array<T> {
    val iter = iterator()
    return Array(size) { iter.next() }
}

inline fun <T> Sequence<T>.peek(crossinline peekingAction: (T) -> Unit) =
    map {
        peekingAction(it)
        it
    }