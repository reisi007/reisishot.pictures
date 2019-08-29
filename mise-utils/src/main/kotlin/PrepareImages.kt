import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.streams.asSequence

object PrepareImages {

    @JvmStatic
    fun main(args: Array<String>) {
        val folder = Paths.get(args.first())
        val configFile = """
                        title = 
                        tags = [
                        
                        ]
                    """.trimIndent()

        val notepadSessionPath = Paths.get(".", "notepad++.session").normalized
        Files.newBufferedWriter(notepadSessionPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { notepadSession ->
            notepadSession appendXml XmlElement("NotepadPlus") {
                it addChild XmlElement("Session", "activeView" to "0") {
                    it addChild XmlElement("mainView", "activeIndex" to "0") {
                        Files.list(folder).asSequence().filterNotNull()
                                .filter { it.toString().let { it.endsWith("jpg", ignoreCase = true) || it.endsWith("jpeg", ignoreCase = true) } }
                                .map { it.resolveSibling("${it.filenameWithoutExtension}.conf") }
                                .filter { !Files.exists(it) }
                                .map { it.normalized }
                                .forEach { p ->
                                    Files.newBufferedWriter(p).use { it.write(configFile) }
                                    it addChild XmlElement("File",
                                            "firstVisibleLine" to "0",
                                            "lang" to "JavaScript",
                                            "filename" to p.toString())
                                }
                    }
                }
            }
            println("Writing Notepad++ session information to:\n$notepadSessionPath")
        }
    }
}

val Path.normalized
    get() = toAbsolutePath().normalize()

@DslMarker
annotation class XmlTagMarker

@XmlTagMarker
class XmlElement(private val name: String, private vararg val attributes: Pair<String, String>, private val addChildrenFunction: (XmlElement) -> Unit = {}) {
    private val children = mutableListOf<XmlElement>()

    init {
        addChildrenFunction(this)
    }

    @XmlTagMarker
    infix fun addChild(element: XmlElement) = children.add(element)


    fun appendTo(appendable: Appendable) {
        appendable.append("<$name")
        attributes.forEach { (key, value) ->
            appendable.append(""" $key="$value" """)
        }
        appendable.append(">")
        children.forEach {
            it.appendTo(appendable)
        }
        appendable.append("</$name>")
    }
}

infix fun Appendable.appendXml(xmlElement: XmlElement) = xmlElement.appendTo(this)