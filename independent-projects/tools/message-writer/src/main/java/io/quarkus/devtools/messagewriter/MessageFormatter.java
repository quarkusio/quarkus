package io.quarkus.devtools.messagewriter;

public class MessageFormatter {

    public enum Format {
        RED("\u001B[91m"),
        GREEN("\u001b[32m"),
        BLUE("\u001b[94m"),
        RESET("\u001b[39m"),
        UNDERLINE("\u001b[4m"),
        NO_UNDERLINE("\u001b[24m"),
        BOLD("\u001b[1m"),
        NO_BOLD("\u001b[22m");

        private final String code;

        Format(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }
    }

    public static String format(Format format, String text) {
        return format + text + Format.RESET;
    }

}