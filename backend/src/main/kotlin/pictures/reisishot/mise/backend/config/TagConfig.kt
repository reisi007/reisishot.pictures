package pictures.reisishot.mise.backend.config

typealias AdditionalTagConfig = MutableMap<TargetTag, MutableSet<NewTag>>
typealias TargetTag = String
typealias NewTag = String

data class TagConfig(
    val additionalTagConfig: AdditionalTagConfig = mutableMapOf()
)

class TagConfigBuilder(
    val entries: MutableList<Pair<NewTag, Array<TargetTag>>> = mutableListOf()
)

fun TagConfig.additionalTags(action: TagConfigBuilder.() -> Unit) {
    val builder = TagConfigBuilder().apply(action)


    val data = builder.entries
        .groupingBy { it.first }
        .aggregate { _, prevAcc: MutableSet<TargetTag>?, (_, elements), _ ->
            (prevAcc ?: mutableSetOf()).apply {
                addAll(elements)
            }
        }

    var curIteration = data.keys

    while (curIteration.isNotEmpty()) {
        val nextIteration = mutableSetOf<NewTag>()

        curIteration.forEach { newTag ->
            val targetTags = data[newTag]
            targetTags?.forEach { target ->
                // Check if a target tag also is a source tag
                if (data.containsKey(target)) {
                    nextIteration += target
                    additionalTagConfig.computeIfAbsent(target)
                    { mutableSetOf() } += newTag
                }
            }
        }

        curIteration = nextIteration
    }
}
