package io.quarkus.platform.catalog.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public final class ExtendedKeywords {
    private ExtendedKeywords() {
    }

    private static final Pattern TOKENIZER_PATTERN = Pattern.compile("\\w+");

    private static final HashSet<String> STOP_WORDS = new HashSet<>(Arrays.asList("the", "and", "you", "that", "was", "for",
            "are", "with", "his", "they", "one",
            "have", "this", "from", "had", "not", "but", "what", "can", "out", "other", "were", "all", "there", "when",
            "your", "how", "each", "she", "which", "their", "will", "way", "about", "many", "then", "them", "would", "enable",
            "these", "her", "him", "has", "over", "than", "who", "may", "down", "been", "more", "implementing", "non",
            "quarkus"));

    public static Set<String> extendsKeywords(String artifactId, String description, List<String> keywords) {
        final HashSet<String> result = new HashSet<>();
        keywords.forEach(it -> result.add(it.toLowerCase(Locale.US)));
        result.add(artifactId.toLowerCase(Locale.US));
        if (!StringUtils.isEmpty(description)) {
            final Matcher matcher = TOKENIZER_PATTERN.matcher(description);
            while (matcher.find()) {
                final String token = matcher.group().toLowerCase(Locale.US);
                if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                    result.add(token);
                }
            }
        }
        return result;
    }
}
