package io.quarkus.runtime.util;

public class JavaVersionUtil {

    private static final boolean IS_GRAALVM_JDK;

    static {
        String vmVendor = System.getProperty("java.vm.vendor");
        IS_GRAALVM_JDK = (vmVendor != null) && vmVendor.startsWith("GraalVM");
    }

    public static boolean isGraalvmJdk() {
        return IS_GRAALVM_JDK;
    }
}
