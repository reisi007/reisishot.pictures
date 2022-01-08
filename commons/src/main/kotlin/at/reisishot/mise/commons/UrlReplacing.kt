package at.reisishot.mise.commons

fun ComplexName.toUrlsafeString() = lowercase()
    .replace(' ', '-')
    .replace("ä", "ae")
    .replace("ö", "oe")
    .replace("ü", "ue")
    .replace("ß", "ss")
