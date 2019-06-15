package pictures.reisishot.mise.backend.generator.gallery

interface ImageInformationRepository {

    val computedTags: Map<TagName, Set<ImageInformation>>

    val imageInformationData: Collection<ImageInformation>
}