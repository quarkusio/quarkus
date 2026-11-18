package io.quarkus.runtime.util;

/**
 * Utilities for command parsers that support double quotes and backslash escapes.
 * These methods do not provide shell-specific escaping.
 */
public final class CommandLineQuoting {

    private CommandLineQuoting() {
    }

    /**
     * Encloses an argument in double quotes, escaping backslashes, double quotes and newlines.
     */
    public static String quote(String argument) {
        return "\"" + argument.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    /**
     * Quotes an argument for a Java or native-image argument file, also escaping other control characters
     * recognized by the argument-file parser.
     */
    public static String quoteForArgumentFile(String argument) {
        return quote(argument).replace("\r", "\\r").replace("\t", "\\t").replace("\f", "\\f");
    }
}
