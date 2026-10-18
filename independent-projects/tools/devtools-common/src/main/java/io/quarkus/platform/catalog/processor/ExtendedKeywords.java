package io.quarkus.platform.catalog.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public static Set<String> extendsKeywords(String artifactId, String name, String shortName, List<String> categories,
            String description, List<String> keywords) {
        final List<String> result = new ArrayList<>();
        result.addAll(expandValue(artifactId, true));
        result.addAll(expandValue(name, false));
        result.addAll(expandValue(shortName, true));
        categories.forEach(it -> result.addAll(expandValue(it, true)));
        result.addAll(keywords);
        if (!StringUtils.isEmpty(description)) {
            final Matcher matcher = TOKENIZER_PATTERN.matcher(description);
            while (matcher.find()) {
                final String token = matcher.group().toLowerCase(Locale.US);
                if (isNotStopWord(token)) {
                    result.add(token);
                }
            }
        }
        return result.stream()
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.US))
                .filter(ExtendedKeywords::isNotStopWord)
                .collect(Collectors.toSet());
    }

    private static boolean isNotStopWord(String token) {
        return token.length() >= 3 && !STOP_WORDS.contains(token);
    }

    private static List<String> expandValue(String value, boolean keepOriginal) {
        if (value == null) {
            return Collections.emptyList();
        }
        String r = value.replaceAll("\\s", "-");
        final List<String> l = new ArrayList<>(Arrays.asList(r.split("-")));
        if (keepOriginal) {
            l.add(r);
        }
        return l;
    }

}
