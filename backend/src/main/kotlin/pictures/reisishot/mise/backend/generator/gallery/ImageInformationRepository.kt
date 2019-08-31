package pictures.reisishot.mise.backend.generator.gallery

interface ImageInformationRepository {

    val computedTags: Map<TagInformation, Set<ImageInformation>>

    val imageInformationData: Collection<ImageInformation>
}

typealias TagUrl = String