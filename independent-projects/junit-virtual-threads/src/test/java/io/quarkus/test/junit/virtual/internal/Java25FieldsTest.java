package io.quarkus.test.junit.virtual.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import io.quarkus.test.junit.virtual.VirtualThreadUnit;

/**
 * Test to verify that Java 24/25 pinning event fields are properly displayed
 * in error messages.
 */
@VirtualThreadUnit
@EnabledForJreRange(min = JRE.JAVA_21)
@DisabledIfSystemProperty(named = "java.runtime.name", matches = ".*Semeru.*", disabledReason = "Semeru doesn't support JFR yet")
public class Java25FieldsTest {

    @Test
    void testJava25FieldsInErrorMessage() {
        // Trigger a pinning event with Java 25 fields
        TestPinJfrEventJava25.pinWithDetails(
                "Native or VM frame on stack",
                "LockSupport.park");

        // The test will fail because we're testing the error message format
        // The actual verification happens in the test below
    }

    /**
     * This test verifies that when a @ShouldNotPin test fails,
     * the error message includes the new fields from Java 24/25.
     */
    @Test
    void verifyFieldsInFailureMessage() {
        // Run a test that should fail and capture the error message
        JUnitEngine.runTestAndAssertFailure(
                io.quarkus.test.junit.virtual.internal.ignore.Java25FieldsHelperTest.class,
                "shouldFailWithJava25Fields",
                "Pinned Reason");

        // Also verify the blocking operation appears
        JUnitEngine.runTestAndAssertFailure(
                io.quarkus.test.junit.virtual.internal.ignore.Java25FieldsHelperTest.class,
                "shouldFailWithJava25Fields",
                "Blocking Operation");
    }
}
