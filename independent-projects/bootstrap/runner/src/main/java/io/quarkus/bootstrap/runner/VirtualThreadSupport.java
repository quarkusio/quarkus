package io.quarkus.bootstrap.runner;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class VirtualThreadSupport {

    private static final int MAJOR_JAVA_VERSION = majorVersionFromJavaSpecificationVersion();

    private static final MethodHandle virtualMh = MAJOR_JAVA_VERSION >= 21 ? findVirtualMH() : null;

    private static MethodHandle findVirtualMH() {
        try {
            return MethodHandles.publicLookup().findVirtual(Thread.class, "isVirtual",
                    MethodType.methodType(boolean.class));
        } catch (Exception e) {
            return null;
        }
    }

    static boolean isVirtualThread() {
        if (virtualMh == null) {
            return false;
        }
        try {
            return (boolean) virtualMh.invokeExact(Thread.currentThread());
        } catch (Throwable t) {
            return false;
        }
    }

    static int majorVersionFromJavaSpecificationVersion() {
        return majorVersion(System.getProperty("java.specification.version", "17"));
    }

    static int majorVersion(String javaSpecVersion) {
        String[] components = javaSpecVersion.split("\\.");
        int[] version = new int[components.length];

        for (int i = 0; i < components.length; ++i) {
            version[i] = Integer.parseInt(components[i]);
        }

        if (version[0] == 1) {
            assert version[1] >= 6;
            return version[1];
        } else {
            return version[0];
        }
    }
}
