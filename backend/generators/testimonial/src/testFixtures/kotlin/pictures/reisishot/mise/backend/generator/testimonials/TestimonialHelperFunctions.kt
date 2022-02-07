package pictures.reisishot.mise.backend.generator.testimonials

import pictures.reisishot.mise.backend.config.PathInformation
import pictures.reisishot.mise.commons.withChild
import kotlin.io.path.createDirectories

fun writePath(pathInformation: PathInformation, vararg ids: String) {
    with(pathInformation.sourceFolder withChild TestimonialLoaderImpl.INPUT_FOLDER_NAME) {
        this.createDirectories()
        ids.forEach { id ->
            createTestimonial(id)
        }
    }
}

fun createTestimonialLoader(paths: PathInformation) =
    TestimonialLoaderImpl.fromPath(paths.sourceFolder)
