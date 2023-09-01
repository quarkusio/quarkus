package io.quarkus.test.junit;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.jupiter.api.AssertionFailureBuilder;

import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

/**
 * Test handling of {@link AssertionError}s and {@link RuntimeException} in callbacks.
 *
 * @see QuarkusTestCallbacksErrorHandlingTest
 */
public class ErrorThrowingCallback implements QuarkusTestAfterEachCallback {
    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        String throwableType = System.getProperty("quarkus.test.callback.throwableType");

        if ("AssertionError".equalsIgnoreCase(throwableType)) {
            AssertionFailureBuilder
                    .assertionFailure()
                    .expected("a")
                    .actual("b")
                    .reason("Oh no, it broke! Here's an assertion error")
                    .buildAndThrow();
        }

        if ("RuntimeException".equalsIgnoreCase(throwableType)) {
            throw new UncheckedIOException(new IOException("Oh dear, it broke again"));
        }
    }
}
