package pictures.reisishot.mise.backend.generator.gallery

import java.nio.file.Path

interface ImageInformationRepository {

    fun getImageInformation(path: Path): ImageInformation

    val allImageInformationData: Collection<ImageInformation>
}