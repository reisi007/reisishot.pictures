package at.reisishot.mise.config

import at.reisishot.mise.commons.ComplexName
import at.reisishot.mise.commons.ConcurrentSet
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.commons.concurrentSetOf
import at.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.tags.TagInformation

internal fun buildImageInformation(vararg tags: String): ImageInformation {
    return object : ImageInformation {
        override val filename: FilenameWithoutExtension = "test"
        override val categories: ConcurrentSet<ComplexName> = concurrentSetOf()
        override val tags: ConcurrentSet<TagInformation> = concurrentSetOf(tags.map { TagInformation(it) })
        override val exifInformation: Map<ExifdataKey, String> = emptyMap()
    }
}
