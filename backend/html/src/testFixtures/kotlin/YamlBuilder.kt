package pictures.reisishot.mise.backend.htmlparsing

class MutableYaml(private val yaml: MutableMap<String, List<String>>) {
    fun asYaml(): Yaml = yaml

    infix fun String.to(element: List<String>) {
        yaml[this] = element
    }

    infix fun String.to(element: String) {
        this to listOf(element)
    }
}

fun buildYaml(action: MutableYaml.() -> Unit): Yaml = MutableYaml(mutableMapOf()).apply(action).asYaml()
