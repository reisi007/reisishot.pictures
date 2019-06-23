package pictures.reisishot.mise.backend.generator

typealias  Link = String
typealias  LinkText = String

sealed class MenuLink(val uniqueIndex: Int, val href: Link?, val text: LinkText)

class MenuLinkContainerItem(
    uniqueIndex: Int,
    href: Link,
    text: LinkText
) : MenuLink(uniqueIndex, href, text)

class MenuLinkContainer(
    val containerId: String,
    uniqueIndex: Int,
    text: LinkText,
    addChildren: MenuLinkContainer.() -> Unit = {}
) : MenuLink(uniqueIndex, null, text) {
    private val internalChildren = mutableListOf<MenuLinkContainerItem>()
    val children: List<MenuLinkContainerItem> get() = internalChildren

    fun addChild(child: MenuLinkContainerItem) {
        internalChildren += child
    }

    operator fun plusAssign(child: MenuLinkContainerItem) = addChild(child)

    init {
        addChildren(this)
    }
}
