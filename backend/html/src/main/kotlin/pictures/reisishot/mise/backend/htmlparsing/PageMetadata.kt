package pictures.reisishot.mise.backend.htmlparsing

import java.util.Date

data class PageMetadata(
    val order: String,
    val created: Date,
    val edited: Date?
)
