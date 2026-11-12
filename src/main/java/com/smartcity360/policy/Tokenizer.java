package com.smartcity360.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Tokenizer {
    private static final Pattern TOKEN = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*|>=|<=|==|!=|[><()]|-?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)");
    public record Token(String text, int position) { }
    public List<Token> tokenize(String source) {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("Enter a rule first.");
        if (source.length() > 2000) throw new IllegalArgumentException("Keep a rule below 2,000 characters.");
        var tokens = new ArrayList<Token>();
        var matcher = TOKEN.matcher(source);
        int position = 0;
        while (position < source.length()) {
            if (Character.isWhitespace(source.charAt(position))) { position++; continue; }
            matcher.region(position, source.length());
            if (!matcher.lookingAt()) throw new IllegalArgumentException("Unexpected character at position " + (position + 1) + ".");
            tokens.add(new Token(matcher.group(), position + 1));
            position = matcher.end();
            if (tokens.size() > 300) throw new IllegalArgumentException("This rule is too long. Use fewer than 300 tokens.");
        }
        tokens.add(new Token("<end>", source.length() + 1));
        return List.copyOf(tokens);
    }
}
