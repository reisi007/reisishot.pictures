package pictures.reisishot.mise.backend.config

import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.commons.ConcurrentSet
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.exifdata.ExifdataKey


class ExtImageInformation(
    val filename: FilenameWithoutExtension,
    val categories: ConcurrentSet<NameWithUrl>,
    val tags: ConcurrentSet<TagInformation>,
    val exifInformation: Map<ExifdataKey, String>,
)

@Serializable
open class ImageInformation(
    val filename: FilenameWithoutExtension,
    val categories: Set<NameWithUrl>,
    val tags: Set<NameWithUrl>,
)

@Serializable
data class NameWithUrl(val name: String, val url: String)
