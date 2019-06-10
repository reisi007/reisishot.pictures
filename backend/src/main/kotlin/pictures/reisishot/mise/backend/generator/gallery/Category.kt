package pictures.reisishot.mise.backend.generator.gallery

data class Category(
    val name: String,
    val includedTagNames: List<String> = emptyList(),
    val excludedTagNames: List<String> = emptyList(),
    val includeSubcategories: Boolean = false
)