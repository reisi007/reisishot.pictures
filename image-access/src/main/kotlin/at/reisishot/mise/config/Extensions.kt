package at.reisishot.mise.config

import at.reisishot.mise.commons.exists
import at.reisishot.mise.commons.useBufferedReader
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import io.github.config4k.extract
import io.github.config4k.toConfig
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

fun ImageConfig.writeConfig(p: Path) {
    val name = "image"
    val config = toConfig(name)
            .getConfig(name)
            .root()
            .render(ConfigRenderOptions.defaults().apply {
                json = false
                originComments = false
            })
    Files.newBufferedWriter(p, StandardCharsets.UTF_8).use { it.write(config) }
}

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