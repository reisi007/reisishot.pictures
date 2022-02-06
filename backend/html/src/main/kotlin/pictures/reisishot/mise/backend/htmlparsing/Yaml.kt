package pictures.reisishot.mise.backend.htmlparsing

import pictures.reisishot.mise.backend.df_yyyyMMdd

typealias Yaml = Map<String, List<String>>


fun Yaml.getPageMetadata(): PageMetadata? {
    val order = getOrder()
    val created = getString("created")
    val edited = getString("updated")

    val computedOrder = when {
        order != null -> order
        created != null -> if (edited != null) "$edited-$created" else "$created-$created"
        else -> null
    }

    // Ensure we have all required fields
    if (computedOrder == null || created == null)
        return null

    return PageMetadata(
        computedOrder,
        df_yyyyMMdd.parse(created),
        edited?.let { df_yyyyMMdd.parse(it) }
    )
}

fun Yaml.getString(key: String): String? {
    val value = getOrDefault(key, null)
    return value?.firstOrNull()?.trim()
}

fun Yaml.getOrder() = getString("order")
