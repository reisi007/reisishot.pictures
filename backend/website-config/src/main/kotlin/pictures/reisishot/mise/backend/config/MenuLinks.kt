package pictures.reisishot.mise.backend.config

import kotlinx.serialization.Serializable
import java.util.TreeSet

typealias Link = String
typealias LinkText = String

@Serializable
sealed class MenuLink : Comparable<MenuLink> {
    abstract val id: String
    abstract val uniqueIndex: Int
    abstract val text: LinkText
    override fun compareTo(other: MenuLink) = compareValuesBy(
        this,
        other,
        { uniqueIndex },
        { id }
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MenuLink) return false

        if (id != other.id) return false
        if (uniqueIndex != other.uniqueIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + uniqueIndex
        return result
    }

}

@Serializable
class MenuLinkContainerItem(
    override val id: String,
    override val uniqueIndex: Int,
    val href: Link,
    override val text: LinkText,
    val target: String?
) : MenuLink() {
    override fun compareTo(other: MenuLink): Int {
        return if (other is MenuLinkContainerItem) {
            compareValuesBy(this, other, { it.uniqueIndex }, { it.href })
        } else
            super.compareTo(other)
    }

    override fun toString(): String {
        return "MenuLinkContainerItem(uniqueIndex=$uniqueIndex, href='$href', text='$text', target=$target)"
    }

}


@Serializable
class MenuLinkContainer(
    override val id: String,
    override val uniqueIndex: Int,
    override val text: LinkText
) : MenuLink() {
    private var internalChildren: MutableSet<MenuLinkContainerItem> = TreeSet()

    val children: Sequence<MenuLinkContainerItem>
        get() = internalChildren.asSequence().distinct()

    @Suppress("MemberVisibilityCanBePrivate")
    fun addChild(child: MenuLinkContainerItem) {
        internalChildren += child
    }

    operator fun plusAssign(child: MenuLinkContainerItem) = addChild(child)
    override fun toString(): String {
        return "MenuLinkContainer(uniqueIndex=$uniqueIndex, text='$text')"
    }

}
