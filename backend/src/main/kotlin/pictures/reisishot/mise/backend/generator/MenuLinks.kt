package pictures.reisishot.mise.backend.generator

import kotlinx.serialization.Serializable
import java.util.*

typealias  Link = String
typealias  LinkText = String

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

}

@Serializable
class MenuLinkContainerItem(
    override val id: String,
    override val uniqueIndex: Int,
    val href: Link,
    override val text: LinkText,
    val target: String?
) : MenuLink()

@Serializable
class MenuLinkContainer(
    override val id: String,
    override val uniqueIndex: Int,
    override val text: LinkText,
    private val orderFunction: (MenuLinkContainerItem) -> Int,
) : MenuLink() {
    private val internalChildren: MutableMap<Int, SortedSet<MenuLinkContainerItem>> =
        mutableMapOf()

    val children: Sequence<MenuLinkContainerItem>
        get() = internalChildren
            .asSequence()
            .flatMap { (_, v) -> v.asSequence() }

    fun addChild(child: MenuLinkContainerItem) {
        val key = orderFunction(child)
        internalChildren.getOrPut(key) { TreeSet() }.add(child)
    }

    operator fun plusAssign(child: MenuLinkContainerItem) = addChild(child)
}
