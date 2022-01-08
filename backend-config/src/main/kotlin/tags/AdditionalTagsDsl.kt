package pictures.reisishot.mise.backend.config.tags

import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.TagConfigDsl


@TagConfigDsl
fun TagConfig.additionalTags(action: AdditionalTagConfigBuilder.() -> Unit) {
    val data = AdditionalTagConfigBuilder()
        .apply(action)
        .build()

    val additionalTagConfig: AdditionalTagConfig = mutableMapOf()

    var curIteration = data.keys

    while (curIteration.isNotEmpty()) {
        val nextIteration = mutableSetOf<NewTag>()

        curIteration.forEach { newTag ->
            val targetTags = data[newTag]
            targetTags?.forEach { target ->
                // Check if a target tag also is a source tag
                if (data.containsKey(target)) {
                    val newElementsAdded = data[newTag]?.let { newTagTargetTags ->
                        data[target]?.let { transitiveTargetTags ->
                            // Add all transitive target tags to the target tag of new Tag
                            newTagTargetTags.addAll(transitiveTargetTags)
                        }
                    }

                    if (newElementsAdded == true)
                        nextIteration += newTag
                }
                additionalTagConfig.computeIfAbsent(target)
                { mutableSetOf() } += newTag
            }
        }

        curIteration = nextIteration
    }


    computables += object : TagComputable {
        override fun processImage(imageInformation: ImageInformation) {
            val original = imageInformation.tags.toSet()
            original.forEach { sourceTag ->
                additionalTagConfig[sourceTag.name]?.let { newTags ->
                    imageInformation.tags += newTags.map { TagInformation(it, "COMPUTED") }
                }
            }
        }

    }
}
