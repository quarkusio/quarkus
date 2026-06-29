package io.quarkus.test.junit.virtual.internal;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.ShouldPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;

@VirtualThreadUnit
public class LoomUnitExampleTest {

    @Test
    @ShouldNotPin
    void methodShouldNotPin() {
    }

    @Test
    @ShouldNotPin(atMost = 1)
    void methodShouldNotPinAtMost1() {
        TestPinJfrEvent.pin();
    }

    @Test
    @ShouldPin
    void methodShouldPin() {
        TestPinJfrEvent.pin();
    }

    @Test
    void methodNoAnnotation() {
    }

}
