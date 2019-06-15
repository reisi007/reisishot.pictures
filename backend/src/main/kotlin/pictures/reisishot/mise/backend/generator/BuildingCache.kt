package pictures.reisishot.mise.backend.generator

import io.github.config4k.toConfig
import pictures.reisishot.mise.backend.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object BuildingCache {

    private const val TIMESTAMPS_KEY = "timestamps"
    private lateinit var timestampMapPath: Path
    private val stringCacheMap: MutableMap<Path, String> = mutableMapOf()

    private lateinit var oldtimestampMap: MutableMap<Path, ZonedDateTime>
    private lateinit var timestampMap: MutableMap<Path, ZonedDateTime>
    private lateinit var linkCahce: MutableMap<String, MutableMap<String, String>>

    private val dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    fun resetLinkcacheFor(linkType: String) = linkCahce.computeIfAbsent(linkType) { mutableMapOf() }.clear()


    fun addLinkcacheEntryFor(linkType: String, linkKey: String, link: String) =
        linkCahce.computeIfAbsent(linkType) { mutableMapOf() }.put(linkKey, link)


    fun getAsString(p: Path): String =
        stringCacheMap.computeIfAbsent(p) { Files.newBufferedReader(it).use { reader -> reader.readLine() } }

    fun hasFileChanged(p: Path): Boolean = with(p.toAbsolutePath().normalize()) {
        val cachedValue: ZonedDateTime? = oldtimestampMap.get(this)

        val actualValue = fileModifiedDateTime

        val hasChanged = when {
            cachedValue == null && actualValue != null -> {
                // New file
                true
            }
            cachedValue != null && actualValue == null -> {
                // File deleted
                true
            }
            actualValue != null && actualValue > cachedValue -> {
                // File changed
                true
            }
            else -> false
        }
        return hasChanged
    }

    fun setFileChanged(p: Path, time: ZonedDateTime?): Unit =
        p.normalize().toAbsolutePath().let { key ->
            if (time != null)
                timestampMap.put(key, time)
            else
                timestampMap.remove(key)
        }

    internal fun setup(config: WebsiteConfiguration) {
        timestampMapPath = config.inPath withChild "timestamp.cache"
        val timestampList: List<Pair<String, String>> = timestampMapPath.parseConfig(TIMESTAMPS_KEY) ?: emptyList()
        oldtimestampMap = mutableMapOf()
        oldtimestampMap.putAll(
            timestampList.asSequence().map { (path, date) ->
                Paths.get(path) to ZonedDateTime.from(
                    dateTimeFormatter.parse(date)
                )
            }.asIterable()
        )
        timestampMap = HashMap(oldtimestampMap)

    }

    internal fun teardown(config: WebsiteConfiguration) {
        timestampMap.asSequence()
            .map { (path, date) -> path.toString() to date.format(dateTimeFormatter) }
            .toList()
            .toConfig(TIMESTAMPS_KEY)
            .root()
            .render()
            .writeTo(timestampMapPath)
    }
}