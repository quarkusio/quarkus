package io.quarkus.security.webauthn.test;

import io.quarkus.test.AbstractQuarkusExtensionTest;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.QuarkusUnitTest;

public class TestUtil {
    static boolean isTestThread() {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            String className = stackTraceElement.getClassName();
            if (QuarkusUnitTest.class.getName().equals(className)
                    || QuarkusExtensionTest.class.getName().equals(className)
                    || AbstractQuarkusExtensionTest.class.getName().equals(className))
                return true;
        }
        return false;
    }
}
