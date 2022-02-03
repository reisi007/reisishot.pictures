package pictures.reisishot.mise.config

import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.commons.ComplexName
import pictures.reisishot.mise.commons.ConcurrentSet
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.commons.concurrentSetOf
import pictures.reisishot.mise.exifdata.ExifdataKey

internal fun buildImageInformation(vararg tags: String): ImageInformation {
    return object : ImageInformation {
        override val filename: FilenameWithoutExtension = "test"
        override val categories: ConcurrentSet<ComplexName> = concurrentSetOf()
        override val tags: ConcurrentSet<TagInformation> = concurrentSetOf(tags.map { TagInformation(it) })
        override val exifInformation: Map<ExifdataKey, String> = emptyMap()

        override fun toString(): String {
            return """Image "$filename" with tags: ${tags.toList()}"""
        }
    }
}
