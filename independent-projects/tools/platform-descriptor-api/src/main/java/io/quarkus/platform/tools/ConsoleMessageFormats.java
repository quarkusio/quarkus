package io.quarkus.platform.tools;

public final class ConsoleMessageFormats {

    private static final String OK = "\u2705";
    private static final String NOK = "\u274c";
    private static final String NOOP = "\uD83D\uDC4D";

    private ConsoleMessageFormats() {
    }

    public static String nok(String content) {
        return NOK + content;
    }

    public static String ok(String content) {
        return OK + content;
    }

    public static String noop(String content) {
        return NOOP + content;
    }
}
