package pictures.reisishot.mise.backend.generator

typealias  Link = String
typealias  LinkText = String

sealed class MenuLink(val uniqueIndex: Int, val href: Link, val text: LinkText)

class SimpleMenuLink(
    uniqueIndex: Int,
    href: Link,
    text: LinkText
) : MenuLink(uniqueIndex, href, text)

class MenuLinkContainer(
    uniqueIndex: Int,
    href: Link,
    text: LinkText,
    addChildren: MenuLinkContainer.() -> Unit
) : MenuLink(uniqueIndex, href, text) {
    private val internalChildren = mutableListOf<MenuLink>()
    val children: List<MenuLink> get() = internalChildren

    fun addChild(child: MenuLink) {
        internalChildren += child
    }

    operator fun plus(child: MenuLink) = addChild(child)

    init {
        addChildren(this)
    }
}
