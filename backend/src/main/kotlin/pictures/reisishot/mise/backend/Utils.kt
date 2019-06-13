package pictures.reisishot.mise.backend

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import io.github.config4k.extract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path

@ObsoleteCoroutinesApi
suspend fun <E> Iterable<E>.forEachLimitedParallel(callable: (E) -> Unit) = coroutineScope {
    map {
        launch(
            /*TODO, maybe Dispatchers IO is enough... newFixedThreadPoolContext(2 * Runtime.getRuntime().availableProcessors(), "Test")*/
            Dispatchers.IO
        ) { callable(it) }
    }
}

@ObsoleteCoroutinesApi
suspend fun <E> Iterable<E>.forEachParallel(callable: (E) -> Unit) = coroutineScope {
    map { launch { callable(it) } }
}

infix fun Path.withChild(fileOrFolder: String) = resolve(fileOrFolder)

fun <T> Path.useInputstream(vararg openOptions: OpenOption, callable: (InputStream) -> T): T =
    Files.newInputStream(this, *openOptions).use(callable)

fun <T> Path.useBufferedReader(callable: (BufferedReader) -> T): T =
    Files.newBufferedReader(this, Charsets.UTF_8).use(callable)

fun Path.parseConfig(configParseOptions: ConfigParseOptions = ConfigParseOptions.defaults()) =
    useBufferedReader { ConfigFactory.parseReader(it, configParseOptions) }

inline fun <reified T> Path.parseConfig(
    configPath: String,
    vararg fallbackPaths: String,
    configParseOptions: ConfigParseOptions = ConfigParseOptions.defaults()
): T {
    val mainConfig: Config = parseConfig(configParseOptions)
    var lastConfig = mainConfig
    fallbackPaths.forEach {
        val curConfig = parseConfig(configParseOptions)

        lastConfig.withFallback(curConfig)

        lastConfig = curConfig
    }

    return mainConfig.extract(configPath)
}