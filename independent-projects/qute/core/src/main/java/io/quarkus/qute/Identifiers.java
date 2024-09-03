package io.quarkus.qute;

public final class Identifiers {

    /**
     * A valid identifier is a sequence of non-whitespace characters.
     *
     * @param value
     * @return {@code true} if the value represents a valid identifier, {@code false} otherwise
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int offset = 0;
        int length = value.length();
        while (offset < length) {
            int c = value.codePointAt(offset);
            if (!Character.isWhitespace(c)) {
                offset += Character.charCount(c);
                continue;
            }
            return false;
        }
        return true;
    }

}
