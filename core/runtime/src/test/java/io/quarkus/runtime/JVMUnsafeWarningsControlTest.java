package io.quarkus.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link JVMUnsafeWarningsControl}.
 */
public class JVMUnsafeWarningsControlTest {

    @Test
    public void testDisableUnsafeRelatedWarnings() {
        // This test verifies that disableUnsafeRelatedWarnings() doesn't throw exceptions
        // and handles different JDK versions gracefully
        assertDoesNotThrow(() -> JVMUnsafeWarningsControl.disableUnsafeRelatedWarnings());
    }

    @Test
    public void testDisableUnsafeRelatedWarningsMultipleTimes() {
        // Verify that calling the method multiple times doesn't cause issues
        assertDoesNotThrow(() -> {
            JVMUnsafeWarningsControl.disableUnsafeRelatedWarnings();
            JVMUnsafeWarningsControl.disableUnsafeRelatedWarnings();
            JVMUnsafeWarningsControl.disableUnsafeRelatedWarnings();
        });
    }
}
