package io.quarkus.deployment.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionUtil {

    private static final Pattern PATTERN = Pattern.compile("(?:1\\.)?(\\d+)(?:\\..*)?");

    private static boolean IS_JAVA_11_OR_NEWER;

    static {
        Matcher matcher = PATTERN.matcher(System.getProperty("java.version", ""));
        IS_JAVA_11_OR_NEWER = !matcher.matches() || Integer.parseInt(matcher.group(1)) >= 11;

    }

    public static boolean isJava11OrHigher() {
        return IS_JAVA_11_OR_NEWER;
    }
}
