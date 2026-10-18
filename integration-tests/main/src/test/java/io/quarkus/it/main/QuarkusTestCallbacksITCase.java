package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusIntegrationTestExtension;

/**
 * The purpose of this test is simply to ensure that {@link TestContextCheckerBeforeEachCallback}
 * can read {@code @TestAnnotation} without issue.
 * Also checks that {@link SimpleAnnotationCheckerBeforeClassCallback} is executed properly
 */
@ExtendWith({ QuarkusTestCallbacksTestCase.IgnoreCustomExceptions.class, QuarkusIntegrationTestExtension.class })
public class QuarkusTestCallbacksITCase {

    private static boolean throwsCustomException = false;

    @AfterEach
    void afterEachWithTestInfo() {
        if (throwsCustomException) {
            throw new QuarkusTestCallbacksTestCase.CustomException();
        }
    }

    @Test
    @QuarkusTestCallbacksTestCase.TestAnnotation
    @Order(1)
    public void testTestMethodHasAnnotation() {
        assertTrue(TestContextCheckerBeforeEachCallback.testAnnotationChecked);
    }

    @Test
    @Order(2)
    public void testBeforeClass() {
        assertEquals(1, SimpleAnnotationCheckerBeforeClassCallback.count.get());
    }

    @Test
    @QuarkusTestCallbacksTestCase.TestAnnotation
    @Order(3)
    public void testInfoTestCase(TestInfo testInfo) throws NoSuchMethodException {
        assertEquals(testInfo.getTestClass().get(), QuarkusTestCallbacksITCase.class);
        Method testMethod = testInfo.getTestMethod().get();
        assertEquals(testMethod, QuarkusTestCallbacksITCase.class.getDeclaredMethod("testInfoTestCase", TestInfo.class));
        assertEquals(1, testMethod.getAnnotationsByType(QuarkusTestCallbacksTestCase.TestAnnotation.class).length);
        assertEquals(QuarkusTestCallbacksITCase.class, testInfo.getTestClass().get());
    }

    @Test
    @Order(4)
    public void testCallbackContextIsNotFailed() {
        assertFalse(TestContextCheckerBeforeEachCallback.CONTEXT.getTestStatus().isTestFailed());
        // To force the exception in the before each handler.
        throwsCustomException = true;
    }

    @Test
    @Order(5)
    public void testCallbackContextIsFailed() {
        assertTrue(TestContextCheckerBeforeEachCallback.CONTEXT.getTestStatus().isTestFailed());
    }
}
