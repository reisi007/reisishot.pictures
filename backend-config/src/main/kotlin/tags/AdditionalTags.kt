package pictures.reisishot.mise.backend.config

class AdditionalTagConfigBuilder(
    private val entries: MutableList<Pair<NewTag, Array<TargetTag>>> = mutableListOf()
) {
    @TagConfigDsl
    infix fun TargetTag.withTags(tag: NewTag) = withTags(arrayOf(tag))

    @TagConfigDsl
    infix fun TargetTag.withTags(tags: Array<NewTag>) {
        entries += tags.map { Pair(it, arrayOf(this)) }
    }

    @TagConfigDsl
    infix fun NewTag.to(tag: TargetTag) = this to (arrayOf(tag))

    @TagConfigDsl
    infix fun NewTag.to(tags: Array<TargetTag>) {
        entries += Pair(this, tags)
    }

    internal fun build() =
        entries
            .groupingBy { it.first }
            .aggregateTo(mutableMapOf()) { _, prevAcc: MutableSet<TargetTag>?, (_, elements), _ ->
                (prevAcc ?: mutableSetOf()).apply {
                    addAll(elements)
                }
            }
}
