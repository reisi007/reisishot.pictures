package pictures.reisishot.mise.backend.generator.gallery.categories

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.*
import pictures.reisishot.mise.backend.parseConfig
import pictures.reisishot.mise.backend.withChild

class ConfigurableCategoryBuilder() : CategoryBuilder {
    override val builderName: String = "Configurable Categorybuilder"

    private lateinit var categoryConfig: List<CategoryConfig>

    override fun generateCategories(
        imageInformationRepository: ImageInformationRepository,
        websiteConfiguration: WebsiteConfiguration
    ): Sequence<Pair<String, String>> {
        val computedCategories: MutableMap<String, Set<String>> = mutableMapOf()


        var categoriesToCompute: Collection<CategoryConfig> = categoryConfig
        var uncomputableCategories: MutableCollection<CategoryConfig> = mutableListOf()
        var uncomputableSizeBefore: Int//Must start with an negative value for correct computation


        do {
            uncomputableSizeBefore = uncomputableCategories.size
            categoriesToCompute.forEach { curCategory ->
                val categoryImages = mutableSetOf<ImageFilename>()
                val subcategoryImages: Sequence<ImageFilename> =
                    if (curCategory.includeSubcategories) {
                        val subalbumNames = getSubalbumNames(curCategory.name)
                        val canCompute = subalbumNames.all { computedCategories.containsKey(it) }
                        if (!canCompute) {
                            uncomputableCategories.add(curCategory)
                            emptySequence()
                        } else
                            subalbumNames.asSequence().flatMap { computedCategories.getValue(it).asSequence() }

                    } else emptySequence()

                categoryImages += subcategoryImages.toList()
                // Add images by Tag
                categoryImages += imageInformationRepository.allImageInformationData.asSequence()
                    .filter { imageInformationEntry ->
                        curCategory.includedTagNames.any { tagName ->
                            imageInformationEntry.tags.any {
                                tagName.equals(
                                    it,
                                    true
                                )
                            }
                        }
                    }.filter { imageInformationEntry ->
                        imageInformationEntry.tags.none { tagName ->
                            curCategory.excludedTagNames.any {
                                tagName.equals(
                                    it,
                                    true
                                )
                            }
                        }
                    }
                    .map { it.filename }
                    .toList()

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
        .filter { it.startsWith(categoryName, true) }
        .toList()

    override fun setup(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        super.setup(configuration, cache)
        categoryConfig = configuration.inFolder.withChild("categories.conf").parseConfig("categories")
    }

    override fun teardown(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        super.teardown(configuration, cache)
    }
}