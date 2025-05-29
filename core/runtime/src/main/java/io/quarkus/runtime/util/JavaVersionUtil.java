package io.quarkus.runtime.util;

public class JavaVersionUtil {

    private static final boolean IS_GRAALVM_JDK;

    static {
        String vmVendor = System.getProperty("java.vm.vendor");
        IS_GRAALVM_JDK = (vmVendor != null) && vmVendor.startsWith("GraalVM");
    }

    @Deprecated
    public static boolean isJava17OrHigher() {
        return Runtime.version().major() >= 17;
    }

    @Deprecated
    public static boolean isJava19OrHigher() {
        return Runtime.version().major() >= 19;
    }

    @Deprecated
    public static boolean isJava21OrHigher() {
        return Runtime.version().major() >= 21;
    }

    public static boolean isGraalvmJdk() {
        return IS_GRAALVM_JDK;
    }
}
