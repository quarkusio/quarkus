package io.quarkus.it.main;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

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

    @Target({ METHOD })
    @Retention(RUNTIME)
    public @interface TestAnnotation {
    }
}
