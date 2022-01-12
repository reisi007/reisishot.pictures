package pictures.reisishot.mise.backend.generator.pages

import java.util.*

data class PageMetadata(
    val order: String,
    val created: Date,
    val edited: Date?
)

fun Yaml.getPageMetadata(): PageMetadata? {
    val order = getOrder()
    val created = getString("created")
    val edited = getString("updated")

    val computedOrder = order ?: edited ?: created
    // Ensure we have all required fields
    if (computedOrder == null || created == null)
        return null
    return PageMetadata(
        computedOrder,
        pictures.reisishot.mise.backend.df_yyyMMdd.parse(created),
        edited?.let { pictures.reisishot.mise.backend.df_yyyMMdd.parse(it) }
    )
}
