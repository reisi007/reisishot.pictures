package pictures.reisishot.mise.backend.config

import pictures.reisishot.mise.backend.config.category.CategoryComputable
import pictures.reisishot.mise.backend.config.category.CategoryConfigRoot
import pictures.reisishot.mise.backend.config.category.CategoryMatcher
import pictures.reisishot.mise.backend.config.category.NewCategoryConfig
import pictures.reisishot.mise.backend.config.tags.TagInformation

@CategoryConfigDsl
fun buildCategoryConfig(action: CategoryConfigRoot.() -> Unit): CategoryConfigRoot =
    mutableSetOf<CategoryComputable>().also(action)

@CategoryConfigDsl
fun CategoryConfigRoot.withSubCategory(
    name: String,
    action: NewCategoryConfig.() -> Unit
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
fun NewCategoryConfig.withSubCategory(
    name: String,
    action: NewCategoryConfig.() -> Unit
) {
    subcategories += categoryOf(name, this, action)
}

@CategoryConfigDsl
inline fun NewCategoryConfig.withComputedSubCategories(
    name: String,
    instanceCreator: (String/*Name*/, NewCategoryConfig/*Base*/) -> CategoryComputable
) {
    subcategories += instanceCreator(name, this)
}

@CategoryConfigDsl
fun categoryOf(name: String, base: NewCategoryConfig? = null, action: NewCategoryConfig.() -> Unit): NewCategoryConfig {
    val realName = if (base != null) "${base.name}/$name" else name
    return NewCategoryConfig(realName).apply(action)
}

@CategoryConfigDsl
fun NewCategoryConfig.includeTagsAndSubcategories(vararg allowedTags: String) = complexMatchOr(
    buildIncludeSubdirectoriesMatcher(),
    buildIncludeTagsMatcher(*allowedTags)
)

@CategoryConfigDsl
fun buildIncludeTagsMatcher(vararg allowedTags: String): CategoryMatcher =
    { i -> allowedTags.any { i.tags.contains(TagInformation(it)) } }

@CategoryConfigDsl
fun NewCategoryConfig.excludedTags(vararg allowedTags: String) {
    matcher = buildExcludeMatcher(*allowedTags)
}

@CategoryConfigDsl
fun CategoryComputable.buildIncludeSubdirectoriesMatcher(): CategoryMatcher =
    { i -> subcategories.any { it.images.contains(i) } }

@CategoryConfigDsl
fun buildExcludeMatcher(vararg allowedTags: String): CategoryMatcher =
    { i -> allowedTags.none { i.tags.contains(TagInformation(it)) } }

@CategoryConfigDsl
fun NewCategoryConfig.complexMatchAnd(vararg matchedByAnd: CategoryMatcher) {
    matcher = { imageInformation ->
        matchedByAnd.all { it(imageInformation) }
    }
}

@CategoryConfigDsl
fun NewCategoryConfig.complexMatchOr(vararg matchedByAnd: CategoryMatcher) {
    matcher = { imageInformation ->
        matchedByAnd.any { it(imageInformation) }
    }
}
