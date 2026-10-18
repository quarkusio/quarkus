package io.quarkus.test.junit.virtual.internal.ignore;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;
import io.quarkus.test.junit.virtual.internal.TestPinJfrEventJava25;

@VirtualThreadUnit
public class Java25FieldsHelperTest {

    @Test
    @ShouldNotPin
    void shouldFailWithJava25Fields() {
        // Trigger a pinning event with Java 25 fields
        TestPinJfrEventJava25.pinWithDetails(
                "Native or VM frame on stack",
                "LockSupport.park");
    }
}
