package at.reisishot.mise.backend.config

import kotlinx.serialization.Serializable

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
    override val text: LinkText
) : MenuLink() {
    private var internalChildren: List<MenuLinkContainerItem> = listOf()

    val children: Sequence<MenuLinkContainerItem>
        get() = internalChildren.asSequence()


    @Suppress("MemberVisibilityCanBePrivate")
    fun addChild(child: MenuLinkContainerItem) {
        internalChildren = (internalChildren.asSequence() + child)
            .sorted()
            .distinct()
            .toList()
    }

    operator fun plusAssign(child: MenuLinkContainerItem) = addChild(child)
}
