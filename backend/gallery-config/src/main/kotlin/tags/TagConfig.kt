package pictures.reisishot.mise.backend.config.tags

import kotlinx.coroutines.runBlocking
import pictures.reisishot.mise.backend.config.ExtImageInformation
import pictures.reisishot.mise.backend.config.TagConfigDsl
import pictures.reisishot.mise.commons.forEachParallel

data class TagConfig(
    private val computables: MutableList<TagComputable> = mutableListOf()
) {
    fun computeTags(images: List<ExtImageInformation>): Unit = runBlocking {
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

    fun processImage(imageInformation: ExtImageInformation)
}
