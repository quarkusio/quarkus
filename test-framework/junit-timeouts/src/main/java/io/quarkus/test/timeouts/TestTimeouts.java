package io.quarkus.test.timeouts;

import java.time.Duration;

/**
 * The shared timeout tests should use for operations that are expected to succeed, for example
 * {@code uni.await().atMost(TestTimeouts.get())}.
 * <p>
 * The value is configured in {@code test-timeouts.properties} at the root of the repository, see
 * {@link TimeoutResolver} for the lookup rules. Do not use this for timeouts that are part of what a test checks
 * (such as "nothing must arrive within 2 seconds").
 */
public final class TestTimeouts {

    /**
     * Set by {@link TestTimeoutsExtension} to the value resolved for the running test. A system property is used so
     * that this works when the test code is loaded by a different classloader than the extension.
     */
    static final String CURRENT_PROPERTY = "quarkus.test.timeout.current";

    private TestTimeouts() {
    }

    /**
     * @return the timeout for the currently running test
     */
    public static Duration get() {
        String current = System.getProperty(CURRENT_PROPERTY);
        if (current != null) {
            return Duration.parse(current);
        }
        // outside of a test, for example in a static initializer
        return TimeoutResolver.global();
    }
}
