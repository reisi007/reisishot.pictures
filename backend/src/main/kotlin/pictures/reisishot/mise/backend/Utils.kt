package pictures.reisishot.mise.backend

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import io.github.config4k.extract
import kotlinx.coroutines.*
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.swing.ImageIcon

@ObsoleteCoroutinesApi
suspend fun <E> Iterable<E>.forEachLimitedParallel(
    dispatcher: CoroutineDispatcher = newFixedThreadPoolContext(
        2 * Runtime.getRuntime().availableProcessors(),
        "Foreach"
    ), callable: suspend (E) -> Unit
) = coroutineScope {
    map {
        launch(dispatcher) { callable(it) }
    }
}

@ObsoleteCoroutinesApi
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
    if (Files.exists(this))
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