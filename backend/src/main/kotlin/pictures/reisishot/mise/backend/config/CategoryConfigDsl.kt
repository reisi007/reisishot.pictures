package pictures.reisishot.mise.backend.config

@ConfigDsl
fun buildCategoryConfig(action: CategoryConfigRoot.() -> Unit): CategoryConfigRoot =
    mutableSetOf<CategoryComputable>().also(action)

@ConfigDsl
fun CategoryConfigRoot.withSubCategory(
    name: String,
    action: NewCategoryConfig.() -> Unit
) {
    add(categoryOf(name, null, action))
}


@ConfigDsl
inline fun CategoryConfigRoot.withComputedSubCategories(
    name: String,
    instanceCreator: (String/*Name*/) -> CategoryComputable
) {
    add(instanceCreator(name))
}

@ConfigDsl
fun NewCategoryConfig.withSubCategory(
    name: String,
    action: NewCategoryConfig.() -> Unit
) {
    subcategories += categoryOf(name, this, action)
}

@ConfigDsl
inline fun NewCategoryConfig.withComputedSubCategories(
    name: String,
    instanceCreator: (String/*Name*/, NewCategoryConfig/*Base*/) -> CategoryComputable
) {
    subcategories += instanceCreator(name, this)
}

@ConfigDsl
fun categoryOf(name: String, base: NewCategoryConfig? = null, action: NewCategoryConfig.() -> Unit): NewCategoryConfig {
    val realName = if (base != null) "${base.name}/$name" else name
    return NewCategoryConfig(realName).apply(action)
}

@ConfigDsl
fun NewCategoryConfig.includeTagsAndSubcategories(vararg allowedTags: String) = complexMatchOr(
    buildIncludeSubdirectoriesMatcher(),
    buildIncludeTagsMatcher(*allowedTags)
)

@ConfigDsl
fun buildIncludeTagsMatcher(vararg allowedTags: String): CategoryMatcher =
    { i -> allowedTags.any { i.tags.contains(it) } }

@ConfigDsl
fun NewCategoryConfig.excludedTags(vararg allowedTags: String) {
    matcher = buildExcludeMatcher(*allowedTags)
}

@ConfigDsl
fun CategoryComputable.buildIncludeSubdirectoriesMatcher(): CategoryMatcher =
    { i -> subcategories.any { it.images.contains(i) } }

@ConfigDsl
fun buildExcludeMatcher(vararg allowedTags: String): CategoryMatcher =
    { i -> allowedTags.none { i.tags.contains(it) } }

@ConfigDsl
fun NewCategoryConfig.complexMatchAnd(vararg matchedByAnd: CategoryMatcher) {
    matcher = { imageInformation ->
        matchedByAnd.all { it(imageInformation) }
    }
}

@ConfigDsl
fun NewCategoryConfig.complexMatchOr(vararg matchedByAnd: CategoryMatcher) {
    matcher = { imageInformation ->
        matchedByAnd.any { it(imageInformation) }
    }
}
