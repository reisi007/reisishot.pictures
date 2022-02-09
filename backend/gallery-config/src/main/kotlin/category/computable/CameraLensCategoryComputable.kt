package pictures.reisishot.mise.backend.config.category.computable

import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.LocaleProvider
import pictures.reisishot.mise.backend.config.category.CategoryComputable
import pictures.reisishot.mise.backend.config.category.NoOpComputable
import pictures.reisishot.mise.backend.config.tags.computable.ExifTagComputable.Companion.TAG_CAMERA
import pictures.reisishot.mise.backend.config.tags.computable.ExifTagComputable.Companion.TAG_LENS
import pictures.reisishot.mise.commons.*
import java.util.concurrent.ConcurrentHashMap

class CameraLensCategoryComputable(
    private val name: String,
    private val baseName: String? = null,
    _defaultImages: () -> Map<Pair<String?, String?>, FilenameWithoutExtension> = { emptyMap() }
) : CategoryComputable {

    init {
        requireNotNull(name.ifBlank { null }) { "Name must not be blank" }
    }

    private val defaultImages = _defaultImages()
    override val complexName: String
        get() = (if (baseName == null) "" else "${baseName.trim()}/") + name.trim()
    override val categoryName by lazy { CategoryName(complexName) }
    override val defaultImage: FilenameWithoutExtension? by lazy { defaultImages[null to null] }
    override val images: ConcurrentSet<ImageInformation> = concurrentSetOf()
    override val subcategories: MutableSet<CategoryComputable>
        get() = cameraMap.values.toMutableSet()
    override val visible: Boolean
        get() = false
    private val cameraMap = ConcurrentHashMap<String, CameraMatcher>()

    override fun matchImage(imageToProcess: ImageInformation, localeProvider: LocaleProvider) {
        val found = imageToProcess.tags.asSequence()
            .filter { it.type == TAG_CAMERA || it.type == TAG_LENS }
            .map { it.type to it.name }
            .toMap()

        if (found.size != 2)
            return

        val camera = found[TAG_CAMERA]
        val lens = found[TAG_LENS]

        if (camera == null || lens == null)
            return

        val cameraMatcher = cameraMap.computeIfAbsent(camera) { CameraMatcher(complexName, defaultImages, camera) }
        val lensMatcher =
            cameraMatcher.lensMap.computeIfAbsent(lens) {
                CameraLensMatcher(
                    cameraMatcher.complexName,
                    defaultImages,
                    camera,
                    lens
                )
            }

        sequenceOf(this, cameraMatcher, lensMatcher).forEach {
            it.images += imageToProcess
            imageToProcess.categories += it.complexName
        }
    }
}

private const val LOTS_OF_IMAGES = 9999999

private class CameraMatcher(
    baseName: String,
    private val defaultImages: Map<Pair<String?, String?>, FilenameWithoutExtension>,
    cameraName: String
) : NoOpComputable() {
    override val complexName: String = "$baseName/${cameraName.replace('/', ' ').toUrlsafeString()}"
    override val categoryName: CategoryName by lazy {
        CategoryName(complexName, "${(LOTS_OF_IMAGES - images.size)}$cameraName", cameraName)
    }
    override val images: ConcurrentSet<ImageInformation> = concurrentSetOf()
    override val defaultImage: FilenameWithoutExtension? by lazy { defaultImages[cameraName to null] }
    override val subcategories: MutableSet<CategoryComputable>
        get() = lensMap.values.toMutableSet()

    val lensMap = ConcurrentHashMap<String, CameraLensMatcher>()
}

private class CameraLensMatcher(
    baseName: String,
    private val defaultImages: Map<Pair<String?, String?>, FilenameWithoutExtension>,
    cameraName: String,
    lensName: String
) : NoOpComputable() {
    override val complexName: String = "$baseName/${lensName.replace('/', ' ').toUrlsafeString()}"
    override val categoryName: CategoryName by lazy {
        CategoryName(complexName, "${(LOTS_OF_IMAGES - images.size)}$cameraName", "$cameraName und dem $lensName")
    }
    override val images: ConcurrentSet<ImageInformation> = concurrentSetOf()
    override val defaultImage: FilenameWithoutExtension? by lazy { defaultImages[cameraName to lensName] }
    override val subcategories: MutableSet<CategoryComputable> = mutableSetOf()

}
