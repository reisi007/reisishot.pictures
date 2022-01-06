package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.toFriendlyPathName
import kotlinx.serialization.Serializable

@Serializable
class TagInformation(val name: String, val type: String = "MANUAL") : Comparator<TagInformation> {
    val url by lazy { name.toFriendlyPathName() }

    override fun compare(o1: TagInformation?, o2: TagInformation?): Int = o1!!.name.compareTo(o2!!.name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagInformation

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "TagName(name='$name')"
    }
}
