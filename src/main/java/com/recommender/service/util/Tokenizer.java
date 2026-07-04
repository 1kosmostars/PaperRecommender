package com.recommender.service.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Minimal whitespace/punctuation tokenizer for BM25 indexing.
 * Lowercases and splits on any non-alphanumeric character; drops tokens shorter than 2 chars.
 */
public final class Tokenizer {

    private static final int MIN_TOKEN_LEN = 2;

    private Tokenizer() {}

    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(t -> t.length() >= MIN_TOKEN_LEN)
                .collect(Collectors.toList());
    }

    /** Tokenize title + abstract concatenated — standard unit for paper indexing. */
    public static List<String> tokenizePaper(String title, String abstractText) {
        String combined = (title == null ? "" : title)
                + " "
                + (abstractText == null ? "" : abstractText);
        return tokenize(combined);
    }
}
