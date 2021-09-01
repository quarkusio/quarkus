package io.quarkus.it.main;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import io.quarkus.test.junit.QuarkusTest;

/**
 * The purpose of this test is simply to ensure that {@link SimpleAnnotationCheckerBeforeEachCallback}
 * can read {@code @TestAnnotation} without issue.
 * Also checks that {@link SimpleAnnotationCheckerBeforeClassCallback} is executed properly
 */
@QuarkusTest
public class QuarkusTestCallbacksTestCase {

    @BeforeAll
    static void beforeAllWithTestInfo(TestInfo testInfo) {
        checkBeforeOrAfterAllTestInfo(testInfo);
    }

    @AfterAll
    static void afterAllWithTestInfo(TestInfo testInfo) {
        checkBeforeOrAfterAllTestInfo(testInfo);
    }

    private static void checkBeforeOrAfterAllTestInfo(TestInfo testInfo) {
        assertNotNull(testInfo);
        assertEquals(QuarkusTestCallbacksTestCase.class, testInfo.getTestClass().get());
        assertFalse(testInfo.getTestMethod().isPresent());
    }

    @BeforeEach
    void beforeEachWithTestInfo(TestInfo testInfo) throws NoSuchMethodException {
        checkBeforeOrAfterEachTestInfo(testInfo, "beforeEachWithTestInfo");
    }

    @AfterEach
    void afterEachWithTestInfo(TestInfo testInfo) throws NoSuchMethodException {
        checkBeforeOrAfterEachTestInfo(testInfo, "afterEachWithTestInfo");
    }

    private void checkBeforeOrAfterEachTestInfo(TestInfo testInfo, String unexpectedMethodName) throws NoSuchMethodException {
        assertNotNull(testInfo);
        String testMethodName = testInfo.getTestMethod().get().getName();
        assertNotEquals(testMethodName,
                QuarkusTestCallbacksTestCase.class.getDeclaredMethod(unexpectedMethodName, TestInfo.class));
        assertTrue(testMethodName.startsWith("test"));
        assertEquals(QuarkusTestCallbacksTestCase.class, testInfo.getTestClass().get());
    }

    @Test
    @TestAnnotation
    @Order(1)
    public void testTestMethodHasAnnotation() {
        assertTrue(SimpleAnnotationCheckerBeforeEachCallback.testAnnotationChecked);
    }

    @Test
    @Order(2)
    public void testBeforeClass() {
        assertEquals(1, SimpleAnnotationCheckerBeforeClassCallback.count.get());
    }

    @Test
    @TestAnnotation
    @Order(3)
    public void testInfoTestCase(TestInfo testInfo) throws NoSuchMethodException {
        assertEquals(testInfo.getTestClass().get(), QuarkusTestCallbacksTestCase.class);
        Method testMethod = testInfo.getTestMethod().get();
        assertEquals(testMethod, QuarkusTestCallbacksTestCase.class.getDeclaredMethod("testInfoTestCase", TestInfo.class));
        assertEquals(1, testMethod.getAnnotationsByType(TestAnnotation.class).length);
        assertEquals(QuarkusTestCallbacksTestCase.class, testInfo.getTestClass().get());
    }

    @Target({ METHOD })
    @Retention(RUNTIME)
    public @interface TestAnnotation {
    }
}
