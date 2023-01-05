package pictures.reisishot.mise.config

import pictures.reisishot.mise.backend.config.ExtImageInformation
import pictures.reisishot.mise.backend.config.LocaleProvider
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.commons.concurrentSetOf
import pictures.reisishot.mise.exifdata.ExifdataKey
import java.util.Locale

fun buildImageInformation(
    tags: Set<String> = setOf(),
    exifInformation: Map<ExifdataKey, String> = emptyMap()
): ExtImageInformation {
    return ExtImageInformation(
        "test",
        concurrentSetOf(),
        concurrentSetOf(tags.map { TagInformation(it) }),
        exifInformation
    )
}

object TestGermanLocaleProvider : LocaleProvider {
    override val locale: Locale
        get() = Locale.GERMANY
}
