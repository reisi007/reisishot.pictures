package pictures.reisishot.mise.backend.config.tags

import at.reisishot.mise.commons.forEachParallel
import kotlinx.coroutines.runBlocking
import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.TagConfigDsl

data class TagConfig(
    private val computables: MutableList<TagComputable> = mutableListOf()
) {
    fun computeTags(images: List<ImageInformation>): Unit = runBlocking {
        images.forEachParallel { iii ->
            computables.forEach { computable ->
                computable.processImage(iii)
            }
        }
    }

    @TagConfigDsl
    fun withComputable(provider: () -> TagComputable) {
        computables += provider()
    }
}

interface TagComputable {

    fun processImage(imageInformation: ImageInformation)
}
