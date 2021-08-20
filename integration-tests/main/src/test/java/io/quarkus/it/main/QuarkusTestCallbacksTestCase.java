package io.quarkus.it.main;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Assertions;
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

    @Test
    @TestAnnotation
    @Order(1)
    public void testTestMethodHasAnnotation() {

    }

    @Test
    @Order(2)
    public void testBeforeClass() {
        assertEquals(1, SimpleAnnotationCheckerBeforeClassCallback.count.get());
    }

    @Test
    @Order(3)
    public void testInfoTestCase(TestInfo testInfo) throws NoSuchMethodException {
        Assertions.assertEquals(testInfo.getTestClass().get(), QuarkusTestCallbacksTestCase.class);
        Assertions.assertEquals(testInfo.getTestMethod().get(),
                QuarkusTestCallbacksTestCase.class.getDeclaredMethod("testInfoTestCase", TestInfo.class));
    }

    @Target({ METHOD })
    @Retention(RUNTIME)
    public @interface TestAnnotation {
    }
}
