package at.reisishot.mise.commons

private val ILLEGAL_CHAR_REGEX = """[:\s\-]+""".toRegex()

fun ComplexName.toUrlsafeString() = lowercase()
    .replace(ILLEGAL_CHAR_REGEX, "-")
    .replace("ä", "ae")
    .replace("ö", "oe")
    .replace("ü", "ue")
    .replace("ß", "ss")
