package pictures.reisishot.mise.backend.config

import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.commons.ConcurrentSet
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.exifdata.ExifdataKey

interface ImageInformation {
    val filename: FilenameWithoutExtension
    val categories: ConcurrentSet<String>
    val tags: ConcurrentSet<TagInformation>
    val exifInformation: Map<ExifdataKey, String>
}
