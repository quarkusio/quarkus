package io.quarkus.security.webauthn.test;

import io.quarkus.test.QuarkusUnitTest;

public class TestUtil {
    static boolean isTestThread() {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (stackTraceElement.getClassName().equals(QuarkusUnitTest.class.getName()))
                return true;
        }
        return false;
    }
}
