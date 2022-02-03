package pictures.reisishot.mise.commons

import java.nio.file.Path

typealias FileExtension = String

fun FileExtension.isJpeg() = equals("jpg", true) || equals("jpeg", true)

fun FileExtension.isWebp() = equals("webp", true)

fun FileExtension.isMarkdown() = equals("md", true)

fun FileExtension.isMarkdownPart(type: String) = equals("$type.md", true)

fun FileExtension.isJson() = equals("json", true)

fun FileExtension.isHtml() = equals("html", true)

fun FileExtension.isHtmlPart(type: String) = equals("$type.html", true)

fun FileExtension.isJetbrainsTemp() = contains("__jb_")

fun FileExtension.isTemp() = contains('~')

fun Path.hasExtension(vararg predicates: (FileExtension) -> Boolean) = predicates.any { it(fileExtension) }
fun Path.hasExtension(predicates: List<(FileExtension) -> Boolean>) = predicates.any { it(fileExtension) }
