package pictures.reisishot.mise.backend.generator

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

object BuildingCache {

    private val stringCacheMap: MutableMap<Path, String> = mutableMapOf();

    private val timestampMap: MutableMap<String, Instant> = mutableMapOf();

    fun getAsString(p: Path): String =
        stringCacheMap.computeIfAbsent(p) { Files.newBufferedReader(it).use { reader -> reader.readLine() } }

    fun getFileChangedDateFor(p: Path): Instant? = timestampMap.get(p.normalize().toAbsolutePath().toString())

    fun setFileChangedDateFor(p: Path, time: Instant?) = p.normalize().toAbsolutePath().toString().let { key ->
        if (time != null)
            timestampMap.put(key, time)
        else
            timestampMap.remove(key)
    }
}