package pictures.reisishot.mise.backend.config

import at.reisishot.mise.commons.forEachParallel
import kotlinx.coroutines.runBlocking
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation


data class TagConfig(
    val computable: MutableList<TagComputable> = mutableListOf()
)

fun TagConfig.computeTags(images: List<InternalImageInformation>): Unit = runBlocking {
    images.forEachParallel { iii ->
        computable.forEach { computable ->
            computable.processImage(iii)
        }
    }
}

interface TagComputable {

    fun processImage(imageInformation: InternalImageInformation)
}
