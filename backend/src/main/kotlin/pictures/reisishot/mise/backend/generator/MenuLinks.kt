package pictures.reisishot.mise.backend.generator

import java.util.*

typealias  Link = String
typealias  LinkText = String

sealed class MenuLink(val id: String, val uniqueIndex: Int, val href: Link?, val text: LinkText, val target: String?) : Comparable<MenuLink> {

    override fun compareTo(other: MenuLink) = compareValuesBy(
            this,
            other,
            { uniqueIndex },
            { id }
    )

}

class MenuLinkContainerItem(
        id: String,
        uniqueIndex: Int,
        href: Link,
        text: LinkText,
        target: String?
) : MenuLink(id, uniqueIndex, href, text, target)

class MenuLinkContainer(
        id: String,
        uniqueIndex: Int,
        text: LinkText,
        private val orderFunction: (MenuLinkContainerItem) -> Int,
        addChildren: MenuLinkContainer.() -> Unit = {}
) : MenuLink(id, uniqueIndex, null, text, null) {
    private val internalChildren = mutableMapOf<Int, TreeSet<MenuLinkContainerItem>>()

    val children: Sequence<MenuLinkContainerItem>
        get() = internalChildren
                .asSequence()
                .flatMap { (_, v) -> v.asSequence() }

    fun addChild(child: MenuLinkContainerItem) {
        val key = orderFunction(child)
        internalChildren.computeIfAbsent(key) { TreeSet() }.add(child)
    }

    operator fun plusAssign(child: MenuLinkContainerItem) = addChild(child)

    init {
        addChildren(this)
    }
}
