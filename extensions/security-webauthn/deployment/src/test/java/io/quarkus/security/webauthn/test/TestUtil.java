package io.quarkus.security.webauthn.test;

import io.quarkus.test.QuarkusExtensionTest;

public class TestUtil {
    static boolean isTestThread() {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            String className = stackTraceElement.getClassName();
            if (QuarkusExtensionTest.class.getName().equals(className))
                return true;
        }
        return false;
    }
}
