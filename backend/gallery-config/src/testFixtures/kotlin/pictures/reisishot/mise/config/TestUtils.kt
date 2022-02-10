package pictures.reisishot.mise.config

import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.LocaleProvider
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.commons.ComplexName
import pictures.reisishot.mise.commons.ConcurrentSet
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.commons.concurrentSetOf
import pictures.reisishot.mise.exifdata.ExifdataKey
import java.util.Locale

fun buildImageInformation(
    tags: Set<String> = setOf(),
    exifInformation: Map<ExifdataKey, String> = emptyMap()
): ImageInformation {
    return object : ImageInformation {
        override val filename: FilenameWithoutExtension = "test"
        override val categories: ConcurrentSet<ComplexName> = concurrentSetOf()
        override val tags: ConcurrentSet<TagInformation> = concurrentSetOf(tags.map { TagInformation(it) })
        override val exifInformation: Map<ExifdataKey, String> = exifInformation

        override fun toString(): String {
            return """Image "$filename" with tags: ${tags.toList()}"""
        }
    }
}

object TestGermanLocaleProvider : LocaleProvider {
    override val locale: Locale
        get() = Locale.GERMANY
}
