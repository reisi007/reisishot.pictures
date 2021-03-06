package at.reisishot.mise.commons

import java.nio.file.Path

typealias FileExtension = String

fun FileExtension.isJpeg() = equals("jpg", true) || equals("jpeg", true)

fun FileExtension.isMarkdown() = equals("md", true)

fun FileExtension.isMarkdownPart(type: String) = equals("$type.md", true)

fun FileExtension.isConf() = equals("conf", true)

fun FileExtension.isHtml() = equals("html", true)

fun FileExtension.isHtmlPart(type: String) = equals("$type.html", true)

fun FileExtension.isJetbrainsTemp() = contains("__jb_")

fun FileExtension.isTemp() = contains('~')

fun Path.hasExtension(vararg predicates: (FileExtension) -> Boolean) = fileExtension.isAny(*predicates)

fun FileExtension.isAny(vararg predicates: (FileExtension) -> Boolean) = predicates.any { it(this) }