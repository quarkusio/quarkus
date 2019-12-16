package io.quarkus.qute;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Escapes a characted sequence using a map of replacements.
 */
public final class Escaper {

    private final Map<Character, String> replacements;

    /**
     *
     * @param replacements
     */
    private Escaper(Map<Character, String> replacements) {
        this.replacements = replacements.isEmpty() ? Collections.emptyMap()
                : new HashMap<>(replacements);
    }

    /**
     *
     * @param value
     * @return an escaped value
     */
    public String escape(CharSequence value) {
        Objects.requireNonNull(value);
        if (value.length() == 0) {
            return value.toString();
        }
        for (int i = 0; i < value.length(); i++) {
            String replacement = replacements.get(value.charAt(i));
            if (replacement != null) {
                // In most cases we will not need to escape the value at all
                return doEscape(value, i, new StringBuilder(value.subSequence(0, i)).append(replacement));
            }
        }
        return value.toString();
    }

    private String doEscape(CharSequence value, int index, StringBuilder builder) {
        int length = value.length();
        while (++index < length) {
            char c = value.charAt(index);
            String replacement = replacements.get(c);
            if (replacement != null) {
                builder.append(replacement);
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<Character, String> replacements;

        private Builder() {
            this.replacements = new HashMap<>();
        }

        public Builder add(char c, String replacement) {
            replacements.put(c, replacement);
            return this;
        }

        public Escaper build() {
            return new Escaper(replacements);
        }

    }

}
