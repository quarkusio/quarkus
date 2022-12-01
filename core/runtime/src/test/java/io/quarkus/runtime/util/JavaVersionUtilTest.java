package io.quarkus.runtime.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JavaVersionUtilTest {

    private static final String JAVA_SPECIFICATION_VERSION = "java.specification.version";

    @Test
    void testJava8() {
        testWithVersion("1.8", () -> {
            assertFalse(JavaVersionUtil.isJava11OrHigher());
            assertFalse(JavaVersionUtil.isJava13OrHigher());
        });
    }

    @Test
    void testJava11() {
        testWithVersion("11", () -> {
            assertTrue(JavaVersionUtil.isJava11OrHigher());
            assertFalse(JavaVersionUtil.isJava13OrHigher());
        });
    }

    @Test
    void testJava14() {
        testWithVersion("14", () -> {
            assertTrue(JavaVersionUtil.isJava11OrHigher());
            assertTrue(JavaVersionUtil.isJava13OrHigher());
        });
    }

    @Test
    void testJava17() {
        testWithVersion("17", () -> {
            assertTrue(JavaVersionUtil.isJava11OrHigher());
            assertTrue(JavaVersionUtil.isJava13OrHigher());
        });
    }

    @Test
    void testJava21() {
        testWithVersion("21", () -> {
            assertTrue(JavaVersionUtil.isJava11OrHigher());
            assertTrue(JavaVersionUtil.isJava13OrHigher());
        });
    }

    private void testWithVersion(String javaSpecificationVersion, Runnable test) {
        String previous = System.getProperty(JAVA_SPECIFICATION_VERSION);
        System.setProperty(JAVA_SPECIFICATION_VERSION, javaSpecificationVersion);
        JavaVersionUtil.performChecks();
        try {
            test.run();
        } finally {
            System.setProperty(JAVA_SPECIFICATION_VERSION, previous);
            JavaVersionUtil.performChecks();
        }

    }

}
