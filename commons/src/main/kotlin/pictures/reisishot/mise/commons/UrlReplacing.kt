package pictures.reisishot.mise.commons

private val ILLEGAL_CHAR_REGEX = """[:\s\-]+""".toRegex()

fun ComplexName.toUrlsafeString() =
    replace(ILLEGAL_CHAR_REGEX, "-")
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("Ä", "Ae")
        .replace("Ö", "Oe")
        .replace("Ü", "Ue")
        .replace("ß", "ss")
