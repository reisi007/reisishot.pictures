package pictures.reisishot.mise.backend.htmlparsing

import assertk.assertThat
import assertk.assertions.containsAll
import org.junit.jupiter.api.Test

class YamlBuilderTest {
    @Test
    fun `create YAML with single value`() {
        val list = buildYaml {
            "test" to "value"
        }.asList()

        assertThat(list).containsAll(
            "test" to "value"
        )
    }


    @Test
    fun `create YAML with multiple value`() {
        val list = buildYaml {
            "test" to listOf("a", "b")
        }.asList()

        assertThat(list).containsAll(
            "test" to "a-b"
        )
    }

    fun Yaml.asList() = asSequence()
        .map { (k, v) -> k to v.joinToString("-") }
        .toList()

}
