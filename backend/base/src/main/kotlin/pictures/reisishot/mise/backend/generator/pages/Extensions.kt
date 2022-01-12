package pictures.reisishot.mise.backend.generator.pages

import java.io.StringWriter
import java.io.Writer


internal fun writeToString(action: (Writer) -> Unit) = StringWriter().apply(action).toString()
