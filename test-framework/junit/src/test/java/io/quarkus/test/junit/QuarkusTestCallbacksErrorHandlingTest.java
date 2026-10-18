package io.quarkus.test.junit;

import java.io.UncheckedIOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

/**
 * Generally, {@link AssertionError} is thrown by testing frameworks such as JUnit. However,
 * it does not derive from {@link RuntimeException} hierarchy, and hence separate unwrapping of each type
 * must be handled in {@link io.quarkus.test.junit.AbstractTestWithCallbacksExtension}.
 * <p>
 * Ensuring that <code>AssertionError</code> is at the top of any stack track allows tooling to properly
 * parse and display expected vs actual diffs, etc.
 *
 * @see ErrorThrowingCallback
 */
public class QuarkusTestCallbacksErrorHandlingTest {

    @Test
    public void testAssertionErrorsAreUnwrappedFromCallback() {
        Assertions.assertThrows(AssertionError.class, () -> {
            System.setProperty("quarkus.test.callback.throwableType", "AssertionError");
            MockCallbackExtension extension = new MockCallbackExtension();
            QuarkusTestMethodContext mockContext = new QuarkusTestMethodContext(new Object(), List.of(), null, null);
            extension.invokeAfterEachCallbacks(mockContext);
        });
    }

    @Test
    public void testRuntimeExceptionsAreUnwrappedFromCallback() {
        Assertions.assertThrows(UncheckedIOException.class, () -> {
            System.setProperty("quarkus.test.callback.throwableType", "RuntimeException");
            MockCallbackExtension extension = new MockCallbackExtension();
            QuarkusTestMethodContext mockContext = new QuarkusTestMethodContext(new Object(), List.of(), null, null);
            extension.invokeAfterEachCallbacks(mockContext);
        });
    }

    public static class MockCallbackExtension extends AbstractTestWithCallbacksExtension {

        public MockCallbackExtension() throws ClassNotFoundException {
            populateCallbacks(Thread.currentThread().getContextClassLoader());
        }

    }
}
