package pictures.reisishot.mise.backend.config.tags

import at.reisishot.mise.commons.forEachParallel
import kotlinx.coroutines.runBlocking
import pictures.reisishot.mise.backend.config.ImageInformation

data class TagConfig(
    val computables: MutableList<TagComputable> = mutableListOf()
)

fun TagConfig.computeTags(images: List<ImageInformation>): Unit = runBlocking {
    images.forEachParallel { iii ->
        computables.forEach { computable ->
            computable.processImage(iii)
        }
    }
}

interface TagComputable {

    fun processImage(imageInformation: ImageInformation)
}
