package pictures.reisishot.mise.backend.htmlparsing

import pictures.reisishot.mise.backend.df_yyyMMdd

typealias Yaml = Map<String, List<String>>


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
        df_yyyMMdd.parse(created),
        edited?.let { df_yyyMMdd.parse(it) }
    )
}

fun Yaml.getString(key: String): String? {
    val value = getOrDefault(key, null)
    return value?.firstOrNull()?.trim()
}

fun Yaml.getOrder() = getString("order")
