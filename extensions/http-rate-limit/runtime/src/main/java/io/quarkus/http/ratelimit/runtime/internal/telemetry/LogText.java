package io.quarkus.http.ratelimit.runtime.internal.telemetry;

/**
 * Makes request data safe to log: keys and paths are decoded client input, so a CR LF in them would otherwise start a
 * forged log line, and an escape sequence would drive the terminal that shows the log.
 */
public final class LogText {

    private LogText() {
    }

    /**
     * @return the text with backslashes, control characters and Unicode line separators escaped as in a Java string
     *         literal: a doubled backslash, {@code \r}, {@code \n}, {@code \t}, else a backslash, {@code u} and four hex
     *         digits; the text itself when it contains none
     */
    public static String escape(String text) {
        int i = 0;
        while (i < text.length() && !needsEscape(text.charAt(i))) {
            i++;
        }
        if (i == text.length()) {
            return text;
        }
        StringBuilder escaped = new StringBuilder(text.length() + 16).append(text, 0, i);
        for (; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '\r' -> escaped.append("\\r");
                case '\n' -> escaped.append("\\n");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (needsEscape(c)) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static boolean needsEscape(char c) {
        // U+2028 LINE SEPARATOR and U+2029 PARAGRAPH SEPARATOR: some log viewers and terminals show them as a line break
        return c == '\\' || Character.isISOControl(c) || c == '\u2028' || c == '\u2029';
    }
}
