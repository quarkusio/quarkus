package io.quarkus.quickcli;

/**
 * ANSI text rendering support. Provides picocli-compatible API for styled console output.
 * Strips @|style text|@ markup patterns and renders plain text.
 */
public enum Ansi {
    AUTO,
    ON,
    OFF;

    /** Whether ANSI escape codes should be emitted. */
    public boolean enabled() {
        if (this == OFF) return false;
        if (this == ON) return true;
        // AUTO: check if stdout is a terminal
        return System.console() != null && !System.getenv().containsKey("NO_COLOR");
    }

    /** Styled text that strips @|style ...|@ markup for plain text output. */
    public Text text(String value) {
        return new Text(value, null);
    }

    /** Creates a Text instance, compatible with picocli's Ansi.Text pattern. */
    public static class Text {
        private final String raw;

        public Text(String raw, Object colorScheme) {
            this.raw = raw != null ? raw : "";
        }

        @Override
        public String toString() {
            return stripAnsiMarkup(raw);
        }

        /** Strips picocli-style @|style text|@ markup, leaving only the text content. */
        static String stripAnsiMarkup(String text) {
            if (text == null) return "";
            // Strip @|style text|@ patterns → just the text
            return text.replaceAll("@\\|[^\\s]+ ([^|]*?)\\|@", "$1");
        }
    }
}
