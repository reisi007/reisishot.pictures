package at.reisishot.mise.commons

import kotlinx.coroutines.*
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.swing.ImageIcon
import kotlin.streams.asSequence

suspend inline fun <E> Iterable<E>.forEachLimitedParallel(
    maxThreadCount: Int, noinline callable: suspend (E) -> Unit
): List<Job> {
    return Executors.newFixedThreadPool(
        maxThreadCount.coerceAtMost(Runtime.getRuntime().availableProcessors())
    ).asCoroutineDispatcher().use {
        forEachParallel(it, callable)
    }
}

suspend fun <E> Iterable<E>.forEachParallel(
    dispatcher: CoroutineDispatcher = Dispatchers.Default, callable: suspend (E) -> Unit
) = coroutineScope {
    map { launch(dispatcher) { callable(it) } }
}

suspend fun <K : Comparable<K>, V> Map<K, Collection<V>>.forEachLimitedParallel(
    dispatcher: CoroutineDispatcher? = null,
    callable: suspend (V) -> Unit
) = coroutineScope {
    keys.forEach { priority ->
        get(priority)?.let { generators ->
            coroutineScope {
                if (dispatcher == null)
                    generators.forEachParallel(callable = callable)
                else
                    generators.forEachParallel(dispatcher, callable)
            }
        } ?: throw IllegalStateException("No at.reisishot.mise.commons.list found for priority $priority")
    }
}

infix fun Path.withChild(fileOrFolder: String): Path = resolve(fileOrFolder)
infix fun Path.withChild(fileOrFolder: Path): Path = resolve(fileOrFolder)

inline fun <T> Path.useBufferedReader(callable: (BufferedReader) -> T): T =
    Files.newBufferedReader(this, Charsets.UTF_8).use(callable)

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

val String.filenameWithoutExtension: FilenameWithoutExtension
    get() = substring(0, lastIndexOf('.'))


val Path.fileExtension: FileExtension
    get() = with(fileName.toString()) {
        substring(indexOf('.') + 1)
    }

fun Path.list(): Sequence<Path> = Files.list(this).asSequence()

fun Path.exists() = Files.exists(this)

fun Path.isRegularFile() = Files.isRegularFile(this)


inline fun <reified T> Sequence<T>.toArray(size: Int): Array<T> {
    val iter = iterator()
    return Array(size) { iter.next() }
}

fun Path.isNewerThan(other: Path): Boolean =
    Files.getLastModifiedTime(this) > Files.getLastModifiedTime(other)

private val whiteSpace = """\s""".toRegex()
fun String.toFriendlyPathName(): String {
    return replace(whiteSpace, "-").lowercase(Locale.getDefault())
}

fun Array<String>.runAndWaitOnConsole() {
    ProcessBuilder(*this)
        .inheritIO()
        .start()
        .waitFor()
}

fun Path.toNormalizedString() = asNormalizedPath().toString()

fun Path.asNormalizedPath(): Path = toAbsolutePath().normalize()

inline fun <T> Sequence<T>.peek(crossinline peekingAction: (T) -> Unit) =
    map {
        peekingAction(it)
        it
    }

fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null
fun <T> ListIterator<T>.previousOrNull(): T? = if (hasPrevious()) previous() else null

fun <K, V : Collection<*>> Map<K, V>.prettyPrint() = keys.forEach { k ->
    println(k.toString())
    println(getValue(k).joinToString("\n\t", "\t"))
    println()
}

fun Path.replaceFileExtension(newExt: String) = parent withChild "$fileName.$newExt"

fun <T> concurrentSetOf(): ConcurrentSet<T> = Collections.newSetFromMap(ConcurrentHashMap())
fun <T> concurrentSetOf(vararg elements: T): ConcurrentSet<T> = concurrentSetOf<T>().apply { addAll(elements) }
fun <T> concurrentSetOf(elements: Iterable<T>): ConcurrentSet<T> = concurrentSetOf<T>().apply { addAll(elements) }
fun <T> concurrentSetOf(element: T): ConcurrentSet<T> = concurrentSetOf<T>().apply { add(element) }

typealias ConcurrentSet<T> = MutableSet<T>
