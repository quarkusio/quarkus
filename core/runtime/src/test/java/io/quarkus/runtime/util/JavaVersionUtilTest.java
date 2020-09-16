package io.quarkus.runtime.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JavaVersionUtilTest {

    private static final String JAVA_VERSION = "java.version";

    @Test
    void testJava8() {
        testWithVersion("1.8.0_242", () -> {
            assertFalse(JavaVersionUtil.isJava11OrHigher());
            assertFalse(JavaVersionUtil.isJava13OrHigher());
        });
    }

    @Test
    void testJava11() {
        testWithVersion("11.0.7", () -> {
            assertTrue(JavaVersionUtil.isJava11OrHigher());
            assertFalse(JavaVersionUtil.isJava13OrHigher());
        });
    }

    @Test
    void testJava14() {
        testWithVersion("14.0.1", () -> {
            assertTrue(JavaVersionUtil.isJava11OrHigher());
            assertTrue(JavaVersionUtil.isJava13OrHigher());
        });
    }

    private void testWithVersion(String javaVersion, Runnable test) {
        String previous = System.getProperty(JAVA_VERSION);
        System.setProperty(JAVA_VERSION, javaVersion);
        JavaVersionUtil.performChecks();
        try {
            test.run();
        } finally {
            System.setProperty(JAVA_VERSION, previous);
            JavaVersionUtil.performChecks();
        }

    }

}
