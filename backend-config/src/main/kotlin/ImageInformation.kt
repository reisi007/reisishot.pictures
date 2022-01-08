package pictures.reisishot.mise.backend.config

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.ConcurrentSet
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.backend.config.tags.TagInformation


interface ImageInformation {
    val filename: FilenameWithoutExtension
    val categories: ConcurrentSet<CategoryName>
    val tags: ConcurrentSet<TagInformation>
    val exifInformation: Map<ExifdataKey, String>
}
