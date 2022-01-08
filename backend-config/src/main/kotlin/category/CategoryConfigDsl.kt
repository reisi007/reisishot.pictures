package pictures.reisishot.mise.backend.config.category

import pictures.reisishot.mise.backend.config.CategoryConfigDsl
import pictures.reisishot.mise.backend.config.tags.TagInformation

@CategoryConfigDsl
fun buildCategoryConfig(action: CategoryConfigRoot.() -> Unit): CategoryConfigRoot =
    mutableSetOf<CategoryComputable>().also(action)

@CategoryConfigDsl
fun CategoryConfigRoot.withSubCategory(
    name: String,
    action: CategoryConfig.() -> Unit
) {
    add(categoryOf(name, null, action))
}

@CategoryConfigDsl
inline fun CategoryConfigRoot.withComputedSubCategories(
    name: String,
    instanceCreator: (String/*Name*/) -> CategoryComputable
) {
    add(instanceCreator(name))
}

@CategoryConfigDsl
fun CategoryConfig.withSubCategory(
    name: String,
    action: CategoryConfig.() -> Unit
) {
    subcategories += categoryOf(name, this, action)
}

@CategoryConfigDsl
inline fun CategoryConfig.withComputedSubCategories(
    name: String,
    instanceCreator: (String/*Name*/, CategoryConfig/*Base*/) -> CategoryComputable
) {
    subcategories += instanceCreator(name, this)
}

@CategoryConfigDsl
fun categoryOf(name: String, base: CategoryConfig? = null, action: CategoryConfig.() -> Unit): CategoryConfig {
    val realName = if (base != null) "${base.name}/$name" else name
    return CategoryConfig(realName).apply(action)
}

@CategoryConfigDsl
fun CategoryConfig.includeTagsAndSubcategories(vararg allowedTags: String) = complexMatchOr(
    buildIncludeSubdirectoriesMatcher(),
    buildIncludeTagsMatcher(*allowedTags)
)

@CategoryConfigDsl
fun buildIncludeTagsMatcher(vararg allowedTags: String): CategoryMatcher =
    { i -> allowedTags.any { i.tags.contains(TagInformation(it)) } }

@CategoryConfigDsl
fun CategoryConfig.excludedTags(vararg allowedTags: String) {
    matcher = buildExcludeMatcher(*allowedTags)
}

@CategoryConfigDsl
fun CategoryComputable.buildIncludeSubdirectoriesMatcher(): CategoryMatcher =
    { i -> subcategories.any { it.images.contains(i) } }

@CategoryConfigDsl
fun buildExcludeMatcher(vararg allowedTags: String): CategoryMatcher =
    { i -> allowedTags.none { i.tags.contains(TagInformation(it)) } }

@CategoryConfigDsl
fun CategoryConfig.complexMatchAnd(vararg matchedByAnd: CategoryMatcher) {
    matcher = { imageInformation ->
        matchedByAnd.all { it(imageInformation) }
    }
}

@CategoryConfigDsl
fun CategoryConfig.complexMatchOr(vararg matchedByAnd: CategoryMatcher) {
    matcher = { imageInformation ->
        matchedByAnd.any { it(imageInformation) }
    }
}
