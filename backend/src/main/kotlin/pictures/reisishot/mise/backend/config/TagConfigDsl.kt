package pictures.reisishot.mise.backend.config

@ConfigDsl
fun buildTagConfig(action: TagConfig.() -> Unit): TagConfig =
    TagConfig().also(action)

@ConfigDsl
fun TagConfigBuilder.additionalTags(vararg newEntries: Pair<TargetTag, Array<NewTag>>) {
    entries += newEntries
        .flatMap { (targetTag, newTags) ->
            newTags.asSequence().map { it to targetTag }
        }
}

@ConfigDsl
fun TagConfigBuilder.addToAll(entry: Pair<NewTag, Array<TargetTag>>) {
    entries += entry
}

@ConfigDsl
infix fun TargetTag.withTags(tag: NewTag) = withTags(arrayOf(tag))

@ConfigDsl
infix fun TargetTag.withTags(tags: Array<NewTag>) = Pair(this, tags)

@ConfigDsl
infix fun NewTag.to(tag: TargetTag) = this to (arrayOf(tag))

@ConfigDsl
infix fun NewTag.to(tags: Array<TargetTag>) = Pair(this, tags)
