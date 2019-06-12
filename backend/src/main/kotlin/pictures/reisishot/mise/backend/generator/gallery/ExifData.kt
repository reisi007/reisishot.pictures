package pictures.reisishot.mise.backend.generator.gallery

import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.file.FileSystemDirectory
import com.drew.metadata.jpeg.JpegDirectory

enum class ExifdataKey(val displayName: String, val getValue: (ExifInformation) -> String?) {
    CAMERA_MAKE("Camera Make", { it.exifD0Directory?.getString(ExifIFD0Directory.TAG_MAKE) }),
    CAMERA_MODEL("Camera model", { it.exifD0Directory?.getString(ExifIFD0Directory.TAG_MODEL) }),
    ISO("ISO", { it.exifSubIFDDirectory?.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) }),
    APERTURE("Aperture", { it.exifSubIFDDirectory?.getString(ExifSubIFDDirectory.TAG_APERTURE) }),
    SHUTTER_SPEED("Shutter speed", { it.exifSubIFDDirectory?.getString(ExifSubIFDDirectory.TAG_SHUTTER_SPEED) }),
    FOCAL_LENGTH("Focal length", { it.exifSubIFDDirectory?.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH) }),
    LENS_MODEL("Lens model", {
        it.exifSubIFDDirectory?.getString(ExifSubIFDDirectory.TAG_LENS_MODEL).let { lensName ->
            if (lensName.isNullOrBlank()) {
                val focalLength = FOCAL_LENGTH.getValue(it)
                if (!focalLength.isNullOrBlank())
                    return@let "$focalLength mm"
            }
            return@let lensName
        }
    });

    override fun toString(): String = displayName
}

class ExifInformation(metadata: Metadata) {
    val jpegDirectory: JpegDirectory?
    val exifD0Directory: ExifIFD0Directory?
    val exifSubIFDDirectory: ExifSubIFDDirectory?
    val fileSystemDirectory: FileSystemDirectory?

    init {
        jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory::class.java)
        exifD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
        exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        fileSystemDirectory = metadata.getFirstDirectoryOfType(FileSystemDirectory::class.java)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExifInformation

        if (jpegDirectory != other.jpegDirectory) return false
        if (exifD0Directory != other.exifD0Directory) return false
        if (exifSubIFDDirectory != other.exifSubIFDDirectory) return false
        if (fileSystemDirectory != other.fileSystemDirectory) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jpegDirectory?.hashCode() ?: 0
        result = 31 * result + (exifD0Directory?.hashCode() ?: 0)
        result = 31 * result + (exifSubIFDDirectory?.hashCode() ?: 0)
        result = 31 * result + (fileSystemDirectory?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ExifInformation(jpegDirectory=$jpegDirectory, exifD0Directory=$exifD0Directory, exifSubIFDDirectory=$exifSubIFDDirectory, fileSystemDirectory=$fileSystemDirectory)"
    }

}