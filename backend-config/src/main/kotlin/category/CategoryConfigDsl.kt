package pictures.reisishot.mise.backend.config.category

import at.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.backend.config.CategoryConfigDsl
import pictures.reisishot.mise.backend.config.tags.TagInformation

@CategoryConfigDsl
fun buildCategoryConfig(action: CategoryConfigRoot.() -> Unit): CategoryConfigRoot =
    mutableSetOf<CategoryComputable>().also(action)

@CategoryConfigDsl
fun CategoryConfigRoot.withSubCategory(
    name: String,
    thumbnailImage: FilenameWithoutExtension? = null,
    action: CategoryConfig.() -> Unit
) {
    add(categoryOf(name, thumbnailImage, null, action))
}

@CategoryConfigDsl
fun CategoryConfigRoot.withComputedSubCategories(
    name: String,
    instanceCreator: (String/*Name*/) -> CategoryComputable
) {
    ensureValidName(name)
    add(instanceCreator(name))
}

@CategoryConfigDsl
fun CategoryConfig.withSubCategory(
    name: String,
    thumbnailImage: FilenameWithoutExtension? = null,
    action: CategoryConfig.() -> Unit
) {
    subcategories += categoryOf(name, thumbnailImage, this, action)
}

@CategoryConfigDsl
fun CategoryConfig.withComputedSubCategories(
    name: String,
    instanceCreator: (String/*Name*/, CategoryConfig/*Base*/) -> CategoryComputable
) {
    ensureValidName(name)
    subcategories += instanceCreator(name, this)
}

@CategoryConfigDsl
fun categoryOf(
    name: String,
    thumbnailImage: FilenameWithoutExtension?,
    base: CategoryConfig? = null,
    action: CategoryConfig.() -> Unit
): CategoryConfig {
    ensureValidName(name)
    val realName = if (base != null) "${base.complexName}/$name" else name
    return CategoryConfig(realName, thumbnailImage).apply(action)
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
fun CategoryComputable.buildIncludeSubdirectoriesMatcher(): CategoryMatcher =
    { i -> subcategories.any { it.images.contains(i) } }

@CategoryConfigDsl
fun buildExcludeMatcher(vararg allowedTags: String): CategoryMatcher =
    { i -> allowedTags.none { i.tags.contains(TagInformation(it)) } }

@CategoryConfigDsl
fun matchAnd(vararg matchedByAnd: CategoryMatcher): CategoryMatcher = { imageInformation ->
    matchedByAnd.all { it(imageInformation) }
}

@CategoryConfigDsl
fun matchOr(vararg matchedByAnd: CategoryMatcher): CategoryMatcher = { imageInformation ->
    matchedByAnd.any { it(imageInformation) }
}

@CategoryConfigDsl
fun CategoryConfig.complexMatchAnd(vararg and: CategoryMatcher) = complexMatcher {
    matchAnd(*and)
}

@CategoryConfigDsl
fun CategoryConfig.complexMatchOr(vararg and: CategoryMatcher) = complexMatcher {
    matchOr(*and)
}

@CategoryConfigDsl
fun CategoryConfig.includeSubcategories() = complexMatcher {
    buildIncludeSubdirectoriesMatcher()
}

@CategoryConfigDsl
fun CategoryConfig.excludedTags(vararg allowedTags: String) = complexMatcher {
    buildExcludeMatcher(*allowedTags)
}

private fun ensureValidName(name: String) {
    val isInvalid = name.contains("/")
    if (isInvalid)
        error("$name is not a valid name for a Category")
}
