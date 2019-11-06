package at.reisishot.mise.config

import at.reisishot.mise.commons.exists
import at.reisishot.mise.commons.useBufferedReader
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import io.github.config4k.extract
import java.nio.file.Path

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