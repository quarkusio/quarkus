package io.quarkus.runtime.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionUtil {

    private static final Pattern PATTERN = Pattern.compile("(?:1\\.)?(\\d+)");

    private static boolean isJava11OrNewer;
    private static boolean isJava13OrNewer;
    private static boolean isGraalvmJdk;
    private static boolean isJava16OrOlder;
    private static boolean isJava17OrNewer;
    private static boolean isJava19OrNewer;
    private static boolean isJava21OrNewer;

    static {
        performChecks();
    }

    // visible for testing
    static void performChecks() {
        Matcher matcher = PATTERN.matcher(System.getProperty("java.specification.version", ""));
        if (matcher.matches()) {
            int first = Integer.parseInt(matcher.group(1));
            isJava11OrNewer = first >= 11;
            isJava13OrNewer = first >= 13;
            isJava16OrOlder = first <= 16;
            isJava17OrNewer = first >= 17;
            isJava19OrNewer = first >= 19;
            isJava21OrNewer = first >= 21;
        } else {
            isJava11OrNewer = false;
            isJava13OrNewer = false;
            isJava16OrOlder = false;
            isJava17OrNewer = false;
            isJava19OrNewer = false;
            isJava21OrNewer = false;
        }

        String vmVendor = System.getProperty("java.vm.vendor");
        isGraalvmJdk = (vmVendor != null) && vmVendor.startsWith("GraalVM");
    }

    public static boolean isJava11OrHigher() {
        return isJava11OrNewer;
    }

    public static boolean isJava13OrHigher() {
        return isJava13OrNewer;
    }

    public static boolean isJava16OrLower() {
        return isJava16OrOlder;
    }

    public static boolean isJava17OrHigher() {
        return isJava17OrNewer;
    }

    public static boolean isJava19OrHigher() {
        return isJava19OrNewer;
    }

    public static boolean isJava21OrHigher() {
        return isJava21OrNewer;
    }

    public static boolean isGraalvmJdk() {
        return isGraalvmJdk;
    }
}
