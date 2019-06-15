package pictures.reisishot.mise.backend.generator

import com.google.gson.reflect.TypeToken
import pictures.reisishot.mise.backend.*
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object BuildingCache {

    private const val TIMESTAMPS_KEY = "timestamps"
    private lateinit var timestampMapPath: Path
    private val stringCacheMap: MutableMap<Path, String> = mutableMapOf()

    // Used for querying (all plugins should have the same cache
    private lateinit var oldtimestampMap: MutableMap<String, ZonedDateTime>
    // Used for updating the cache for the next run
    private lateinit var timestampMap: MutableMap<String, ZonedDateTime>
    private lateinit var linkCahce: MutableMap<String, MutableMap<String, String>>

    private val dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    fun resetLinkcacheFor(linkType: String) = linkCahce.computeIfAbsent(linkType) { mutableMapOf() }.clear()


    fun addLinkcacheEntryFor(linkType: String, linkKey: String, link: String) =
        linkCahce.computeIfAbsent(linkType) { mutableMapOf() }.put(linkKey, link)


    fun getAsString(p: Path): String =
        stringCacheMap.computeIfAbsent(p) { Files.newBufferedReader(it).use { reader -> reader.readLine() } }

    fun hasFileChanged(p: Path): Boolean {
        val cachedValue: ZonedDateTime? = oldtimestampMap.get(p.toNormalizedString())

        val actualValue = p.fileModifiedDateTime

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

    fun setFileChanged(p: Path, time: ZonedDateTime?) = with(p.toNormalizedString()) {
        if (time != null)
            timestampMap.put(this, time)
        else
            timestampMap.remove(this)
    }

    internal fun setup(config: WebsiteConfiguration) {
        timestampMapPath = config.inPath withChild "timestamp.cache.json"
        oldtimestampMap =
            timestampMapPath.fromJson(object : TypeToken<HashMap<String, ZonedDateTime>>() {}) ?: mutableMapOf()
        timestampMap = HashMap(oldtimestampMap)
    }

    internal fun teardown(config: WebsiteConfiguration) {
        timestampMap.toJson(timestampMapPath)
    }
}