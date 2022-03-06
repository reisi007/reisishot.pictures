package pictures.reisishot.mise.commons

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.streams.asSequence

suspend fun <E> Iterable<E>.forEachParallel(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    callable: suspend (E) -> Unit
) = coroutineScope {
    forEach { launch(dispatcher) { callable(it) } }
}

suspend fun <E> Sequence<E>.forEachParallel(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    callable: suspend (E) -> Unit
) = coroutineScope {
    forEach { launch(dispatcher) { callable(it) } }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <K : Comparable<K>, V> Map<K, Collection<V>>.forEachLimitedParallel(
    maximum: Int = Runtime.getRuntime().availableProcessors(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    callable: suspend (V) -> Unit
) = coroutineScope {
    keys.forEach { priority ->
        get(priority)?.forEachParallel(
            dispatcher.limitedParallelism(maximum),
            callable
        ) ?: throw IllegalStateException("No at.reisishot.mise.commons.list found for priority $priority")
    }
}


infix fun Path.withChild(fileOrFolder: String): Path = resolve(fileOrFolder)
infix fun Path.withChild(fileOrFolder: Path): Path = resolve(fileOrFolder)

inline fun <T> Path.useBufferedReader(callable: (BufferedReader) -> T): T =
    Files.newBufferedReader(this, Charsets.UTF_8).use(callable)

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

fun Path.isRegularFile() = Files.isRegularFile(this)

fun Path.isNewerThan(other: Path): Boolean =
    Files.getLastModifiedTime(this) > Files.getLastModifiedTime(other)

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

fun <K, V : Collection<*>> Map<K, V>.prettyPrint() = keys.forEach { k ->
    println(k.toString())
    println(getValue(k).joinToString("\n\t", "\t"))
    println()
}

fun Path.replaceFileExtension(newExt: String) = parent withChild "$filenameWithoutExtension.$newExt"

inline fun <reified T> Sequence<T>.toTypedArray() = toList().toTypedArray()
