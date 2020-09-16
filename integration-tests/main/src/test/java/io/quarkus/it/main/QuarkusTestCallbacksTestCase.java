package io.quarkus.it.main;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * The purpose of this test is simply to ensure that {@link SimpleAnnotationCheckerBeforeEachCallback}
 * can read {@code @TestAnnotation} without issue
 */
@QuarkusTest
public class QuarkusTestCallbacksTestCase {

    @Test
    @TestAnnotation
    public void testTestMethodHasAnnotation() {

    }

    @Target({ METHOD })
    @Retention(RUNTIME)
    public @interface TestAnnotation {
    }
}
