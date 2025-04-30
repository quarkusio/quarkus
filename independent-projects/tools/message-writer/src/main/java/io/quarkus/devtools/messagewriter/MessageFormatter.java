package io.quarkus.devtools.messagewriter;

public class MessageFormatter {

    enum Format {
        RED("\u001B[91m"),
        GREEN("\u001b[32m"),
        BLUE("\u001b[94m"),
        RESET_COLOR("\u001b[39m"),
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

    public static String bold(String text) {
        return Format.BOLD + text + Format.NO_BOLD;
    }

    public static String underline(String text) {
        return Format.UNDERLINE + text + Format.NO_UNDERLINE;
    }

    public static String red(String text) {
        return Format.RED + text + Format.RESET_COLOR;
    }

    public static String green(String text) {
        return Format.GREEN + text + Format.RESET_COLOR;
    }

    public static String blue(String text) {
        return Format.BLUE + text + Format.RESET_COLOR;
    }
}