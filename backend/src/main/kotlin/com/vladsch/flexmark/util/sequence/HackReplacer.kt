package com.vladsch.flexmark.util.sequence

class HackReplacer : Escaping.Replacer {
    override fun replace(s: String, sb: StringBuilder) {
        when (s) {
            "&" -> {
                sb.append("&amp;")
            }
            "<" -> {
                sb.append("&lt;")
            }
            ">" -> {
                sb.append("&gt;")
            }
            else -> {
                sb.append(s)
            }
        }
    }

    override fun replace(original: BasedSequence, startIndex: Int, endIndex: Int, textMapper: ReplacedTextMapper) {
        when (original.subSequence(startIndex, endIndex).toString()) {
            "&" -> {
                textMapper.addReplacedText(startIndex, endIndex, PrefixedSubSequence.prefixOf("&amp;", BasedSequence.NULL))
            }
            "<" -> {
                textMapper.addReplacedText(startIndex, endIndex, PrefixedSubSequence.prefixOf("&lt;", BasedSequence.NULL))
            }
            ">" -> {
                textMapper.addReplacedText(startIndex, endIndex, PrefixedSubSequence.prefixOf("&gt;", BasedSequence.NULL))
            }
            else -> {
                textMapper.addOriginalText(startIndex, endIndex)
            }
        }
    }
}