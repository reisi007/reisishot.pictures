package pictures.reisishot.mise.backend

import com.drew.imaging.ImageMetadataReader
import com.thoughtworks.xstream.XStream
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import io.github.config4k.extract
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import pictures.reisishot.mise.backend.generator.gallery.ExifInformation
import pictures.reisishot.mise.backend.generator.gallery.ExifdataKey
import pictures.reisishot.mise.backend.generator.gallery.FilenameWithoutExtension
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.swing.ImageIcon
import kotlin.streams.asSequence

suspend inline fun <E> Iterable<E>.forEachLimitedParallel(
        maxThreadCount: Int, noinline callable: suspend (E) -> Unit
) = forEachParallel(
        newFixedThreadPoolContext(
                Math.min(maxThreadCount, Runtime.getRuntime().availableProcessors()),
                "Foreach"
        ), callable
)

suspend fun <E> Iterable<E>.forEachParallel(
        dispatcher: CoroutineDispatcher = newFixedThreadPoolContext(
                Runtime.getRuntime().availableProcessors(),
                "Foreach"
        ), callable: suspend (E) -> Unit
) = coroutineScope {
    map { launch(dispatcher) { callable(it) } }
}

suspend fun <K : Comparable<K>, V> Map<K, Collection<V>>.forEachLimitedParallel(dispatcher: CoroutineDispatcher? = null, callable: suspend (V) -> Unit) = coroutineScope {
    keys.forEach { priority ->
        get(priority)?.let { generators ->
            coroutineScope {
                if (dispatcher == null)
                    generators.forEachParallel(callable = callable)
                else
                    generators.forEachParallel(dispatcher, callable)
            }
        } ?: throw IllegalStateException("No list found for priority $priority")
    }
}

infix fun Path.withChild(fileOrFolder: String): Path = resolve(fileOrFolder)
infix fun Path.withChild(fileOrFolder: Path): Path = resolve(fileOrFolder)

inline fun <T> Path.useBufferedReader(callable: (BufferedReader) -> T): T =
        Files.newBufferedReader(this, Charsets.UTF_8).use(callable)

fun Path.getConfig(configParseOptions: ConfigParseOptions = ConfigParseOptions.defaults()): Config? =
        if (exists())
            useBufferedReader {
                try {
                    ConfigFactory.parseReader(it, configParseOptions)
                } catch (e: Exception) {
                    System.err.println("Error while reading from $this")
                    throw e
                }
            }
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
    get() = fileName.toString().filenameWithoutExtension

internal val String.filenameWithoutExtension: FilenameWithoutExtension
    get() = substring(0, lastIndexOf('.'))


val Path.fileExtension: FileExtension
    get() = with(fileName.toString()) {
        substring(lastIndexOf('.') + 1)
    }

fun Path.list(): Sequence<Path> = Files.list(this).asSequence()

fun Path.exists() = Files.exists(this)

fun Path.isRegularFile() = Files.isRegularFile(this)

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

inline fun <reified T> Sequence<T>.toArray(size: Int): Array<T> {
    val iter = iterator()
    return Array(size) { iter.next() }
}

internal fun Path.isNewerThan(other: Path): Boolean =
        Files.getLastModifiedTime(this) > Files.getLastModifiedTime(other)

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

private val whiteSpace = """\s""".toRegex()
internal fun String.toFriendlyPathName(): String {
    return replace(whiteSpace, "-").toLowerCase()
}

fun Array<String>.runAndWaitOnConsole() {
    ProcessBuilder(*this)
            .inheritIO()
            .start()
            .waitFor()
}

fun Path.toNormalizedString() = asNormalizedPath().toString()

fun Path.asNormalizedPath(): Path = toAbsolutePath().normalize()

typealias FileExtension = String