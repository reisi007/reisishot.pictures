package pictures.reisishot.mise.backend.generator.gallery.categories

import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.commons.withChild
import at.reisishot.mise.config.parseConfig
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.*
import java.util.concurrent.ConcurrentHashMap

class ConfigurableCategoryBuilder : CategoryBuilder {
    override val builderName: String = "Configurable Category Builder"

    private lateinit var categoryConfigs: List<CategoryConfig>

    override suspend fun generateCategories(
        imageInformationRepository: ImageInformationRepository,
        websiteConfiguration: WebsiteConfiguration
    ): Sequence<Pair<FilenameWithoutExtension, CategoryInformation>> {
        val computedCategories: MutableMap<CategoryConfig, Set<FilenameWithoutExtension>> = ConcurrentHashMap()


        var categoriesToCompute: Collection<CategoryConfig> = categoryConfigs
        var uncomputableCategories: MutableCollection<CategoryConfig> = mutableListOf()
        var uncomputableSizeBefore: Int


        do {
            uncomputableSizeBefore = uncomputableCategories.size
            categoriesToCompute.forEach { curCategory ->
                val categoryImages =
                    if (curCategory.includeSubcategories) {
                        val subalbumNames = getSubalbumNames(curCategory)
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
                        imageInformationRepository.computedTags[TagInformation(tagName)]?.asSequence()
                            ?.map { it.filename } ?: emptySequence()
                    }

                categoryImages -= curCategory.excludedTagNames.asSequence()
                    .flatMap { tagName ->
                        imageInformationRepository.computedTags[TagInformation(tagName)]?.asSequence()
                            ?.map { it.filename } ?: emptySequence()
                    }

                computedCategories.put(curCategory, categoryImages)
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

        return computedCategories.asSequence().flatMap { (categoryConfig, filesInCategory) ->
            categoryConfig.toCategoryInfotmation().let { categoryInformation ->
                filesInCategory.asSequence().map { filename ->
                    filename to categoryInformation
                }
            }
        }
    }

    private fun getSubalbumNames(categoryConfig: CategoryConfig): Collection<CategoryConfig> =
        categoryConfigs.asSequence()
            .filter { it.name.startsWith(categoryConfig.name, true) && categoryConfig.name != it.name }
            .toList()

    override suspend fun setup(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        super.setup(configuration, cache)
        categoryConfigs = configuration.inPath.withChild("categories.conf").let {
            it.parseConfig<List<CategoryConfig>>("categories")
                ?: throw IllegalStateException("Could not find config file \"$it\"!")
        }
    }

    override suspend fun teardown(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        super.teardown(configuration, cache)
    }
}
