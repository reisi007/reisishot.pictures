package pictures.reisishot.mise.backend.generator

import com.google.common.collect.TreeMultiset
import java.util.*

typealias  Link = String
typealias  LinkText = String

sealed class MenuLink(val id: String, val uniqueIndex: Int, val href: Link?, val text: LinkText)

class MenuLinkContainerItem(
        id: String,
        uniqueIndex: Int,
        href: Link,
        text: LinkText
) : MenuLink(id, uniqueIndex, href, text)

class MenuLinkContainer(
        id: String,
        uniqueIndex: Int,
        text: LinkText,
        childComperator: Comparator<MenuLinkContainerItem>,
        addChildren: MenuLinkContainer.() -> Unit = {}
) : MenuLink(id, uniqueIndex, null, text) {
    private val internalChildren = TreeMultiset.create(childComperator)
    val children: Collection<MenuLinkContainerItem> get() = internalChildren

    fun addChild(child: MenuLinkContainerItem) {
        internalChildren += child
    }

    operator fun plusAssign(child: MenuLinkContainerItem) = addChild(child)

    init {
        addChildren(this)
    }
}
