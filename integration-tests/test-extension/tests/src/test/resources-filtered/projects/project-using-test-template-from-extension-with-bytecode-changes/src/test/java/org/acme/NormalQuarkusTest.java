package org.acme;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Sense check - do we see the added annotation without parameterization?
 */
@QuarkusTest
public class NormalQuarkusTest {

    @Test
    void executionAnnotationCheckingTestTemplate() {
        Annotation[] myAnnotations = this.getClass().getAnnotations();
        Assertions.assertTrue(Arrays.toString(myAnnotations).contains("AnnotationAddedByExtension"),
                "The test execution does not see the annotation, only sees " + Arrays.toString(myAnnotations));
    }
}
