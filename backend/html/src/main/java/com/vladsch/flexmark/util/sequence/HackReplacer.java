package com.vladsch.flexmark.util.sequence;

public final class HackReplacer implements Escaping.Replacer {
    public void replace(String s, StringBuilder sb) {
        switch (s) {
            case "&" -> {
                sb.append("&amp;");
            }
            case "<" -> {
                sb.append("&lt;");
            }
            case ">" -> {
                sb.append("&gt;");
            }
            default -> {
                sb.append(s);
            }
        }
    }

    public void replace(BasedSequence original, int startIndex, int endIndex, ReplacedTextMapper textMapper) {

        String value = original.subSequence(startIndex, endIndex).toString();
        switch (value) {
            case "&" -> {
                textMapper.addReplacedText(startIndex, endIndex, PrefixedSubSequence.prefixOf("&amp;", BasedSequence.NULL));
            }
            case "<" -> {
                textMapper.addReplacedText(startIndex, endIndex, PrefixedSubSequence.prefixOf("&lt;", BasedSequence.NULL));
            }
            case ">" -> {
                textMapper.addReplacedText(startIndex, endIndex, PrefixedSubSequence.prefixOf("&gt;", BasedSequence.NULL));
            }
            default -> {
                textMapper.addOriginalText(startIndex, endIndex);
            }
        }
    }
}
