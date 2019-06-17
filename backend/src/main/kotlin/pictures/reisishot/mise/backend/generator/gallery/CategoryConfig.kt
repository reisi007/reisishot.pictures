package pictures.reisishot.mise.backend.generator.gallery

data class CategoryConfig(
    val name: String,
    val url: String,
    val includedTagNames: List<String> = emptyList(),
    val excludedTagNames: List<String> = emptyList(),
    val includeSubcategories: Boolean = false
)