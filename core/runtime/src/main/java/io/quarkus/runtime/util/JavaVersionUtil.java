package io.quarkus.runtime.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionUtil {

    private static final Pattern PATTERN = Pattern.compile("(?:1\\.)?(\\d+)");

    private static boolean IS_JAVA_11_OR_NEWER;
    private static boolean IS_JAVA_13_OR_NEWER;

    static {
        performChecks();
    }

    // visible for testing
    static void performChecks() {
        Matcher matcher = PATTERN.matcher(System.getProperty("java.specification.version", ""));
        if (matcher.matches()) {
            int first = Integer.parseInt(matcher.group(1));
            IS_JAVA_11_OR_NEWER = (first >= 11);
            IS_JAVA_13_OR_NEWER = (first >= 13);
        } else {
            IS_JAVA_11_OR_NEWER = false;
            IS_JAVA_13_OR_NEWER = false;
        }
    }

    public static boolean isJava11OrHigher() {
        return IS_JAVA_11_OR_NEWER;
    }

    public static boolean isJava13OrHigher() {
        return IS_JAVA_13_OR_NEWER;
    }
}
