package org.acme;

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
        Assertions.assertTrue(true);
    }
}
