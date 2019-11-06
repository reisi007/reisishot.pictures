package at.reisishot.mise.config

import at.reisishot.mise.commons.CategoryName

data class ImageConfig(
        val title: String,
        val categoryThumbnail: Set<CategoryName> = emptySet(),
        val tags: Set<String>
)