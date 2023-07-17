package io.quarkus.runtime.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionUtil {

    private static final Pattern PATTERN = Pattern.compile("(?:1\\.)?(\\d+)");

    private static boolean IS_JAVA_11_OR_NEWER;
    private static boolean IS_JAVA_13_OR_NEWER;
    private static boolean IS_GRAALVM_JDK;
    private static boolean IS_JAVA_16_OR_OLDER;
    private static boolean IS_JAVA_17_OR_NEWER;

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
            IS_JAVA_16_OR_OLDER = (first <= 16);
            IS_JAVA_17_OR_NEWER = (first >= 17);
        } else {
            IS_JAVA_11_OR_NEWER = false;
            IS_JAVA_13_OR_NEWER = false;
            IS_JAVA_16_OR_OLDER = false;
            IS_JAVA_17_OR_NEWER = false;
        }

        String vmVendor = System.getProperty("java.vm.vendor");
        IS_GRAALVM_JDK = (vmVendor != null) && vmVendor.startsWith("GraalVM");
    }

    public static boolean isJava11OrHigher() {
        return IS_JAVA_11_OR_NEWER;
    }

    public static boolean isJava13OrHigher() {
        return IS_JAVA_13_OR_NEWER;
    }

    public static boolean isJava16OrLower() {
        return IS_JAVA_16_OR_OLDER;
    }

    public static boolean isJava17OrHigher() {
        return IS_JAVA_17_OR_NEWER;
    }

    public static boolean isGraalvmJdk() {
        return IS_GRAALVM_JDK;
    }
}
