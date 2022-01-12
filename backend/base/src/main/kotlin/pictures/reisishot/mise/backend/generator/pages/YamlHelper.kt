package pictures.reisishot.mise.backend.generator.pages


fun Yaml.getString(key: String): String? {
    val value = getOrDefault(key, null)
    return value?.firstOrNull()?.trim()
}

fun Yaml.getOrder() = getString("order")
