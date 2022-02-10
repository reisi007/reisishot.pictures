package pictures.reisishot.mise.backend.htmlparsing

import pictures.reisishot.mise.backend.df_yyyyMMdd

typealias Yaml = Map<String, List<String>>

fun Yaml.getPageMetadata(): PageMetadata? {
    val edited = getString("updated")
    val created = getString("created") ?: edited ?: return null

    val computedOrder = getOrder() ?: if (edited != null) "$edited-$created" else "$created-$created"

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
