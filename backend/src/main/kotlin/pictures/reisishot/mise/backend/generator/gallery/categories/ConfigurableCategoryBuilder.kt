package pictures.reisishot.mise.backend.generator.gallery.categories

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.*
import pictures.reisishot.mise.backend.parseConfig
import pictures.reisishot.mise.backend.withChild

class ConfigurableCategoryBuilder() : CategoryBuilder {
    override val builderName: String = "Configurable Categorybuilder"

    private lateinit var categoryConfig: List<CategoryConfig>

    override suspend fun generateCategories(
        imageInformationRepository: ImageInformationRepository,
        websiteConfiguration: WebsiteConfiguration
    ): Sequence<Pair<FilenameWithoutExtension, CategoryName>> {
        val computedCategories: MutableMap<CategoryName, Set<FilenameWithoutExtension>> = mutableMapOf()


        var categoriesToCompute: Collection<CategoryConfig> = categoryConfig
        var uncomputableCategories: MutableCollection<CategoryConfig> = mutableListOf()
        var uncomputableSizeBefore: Int


        do {
            uncomputableSizeBefore = uncomputableCategories.size
            categoriesToCompute.forEach { curCategory ->
                val categoryImages =
                    if (curCategory.includeSubcategories) {
                        val subalbumNames = getSubalbumNames(curCategory.name)
                        val canCompute = subalbumNames.all { computedCategories.containsKey(it) }
                        if (!canCompute) {
                            uncomputableCategories.add(curCategory)
                            return@forEach
                        } else
                            subalbumNames.asSequence().flatMap { categoryName ->
                                computedCategories.getValue(categoryName).asSequence()
                            }.toMutableSet()
                    } else mutableSetOf()

                // Add images by Tag
                categoryImages += curCategory.includedTagNames.asSequence()
                    .flatMap { tagName ->
                        imageInformationRepository.computedTags.getValue(tagName).asSequence()
                            .map { it.filenameWithoutExtension }
                    }

                categoryImages -= curCategory.excludedTagNames.asSequence()
                    .flatMap { tagName ->
                        imageInformationRepository.computedTags.getValue(tagName).asSequence()
                            .map { it.filenameWithoutExtension }
                    }

                computedCategories.put(curCategory.name, categoryImages)
            }

            categoriesToCompute = uncomputableCategories
            uncomputableCategories = mutableListOf()
        } while (uncomputableSizeBefore != categoriesToCompute.size /*Uncomputable categories are next round's categories*/)
        if (categoriesToCompute.isNotEmpty())
            throw IllegalStateException(
                "Cannot compute category mapping for categories: " + uncomputableCategories.joinToString(
                    ", "
                )
            )

        return computedCategories.asSequence().flatMap { (categoryName, filesInCategory) ->
            filesInCategory.asSequence().map { filename ->
                filename to categoryName
            }
        }
    }

    private fun getSubalbumNames(categoryName: CategoryName): Collection<CategoryName> = categoryConfig.asSequence()
        .map { it.name }
        .filter { it.startsWith(categoryName, true) && categoryName != it }
        .toList()

    override suspend fun setup(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        super.setup(configuration, cache)
        categoryConfig = configuration.inPath.withChild("categories.conf").let {
            it.parseConfig("categories") ?: throw IllegalStateException("Could not find config file \"$it\"!")
        }
    }

    override suspend fun teardown(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        super.teardown(configuration, cache)
    }
}