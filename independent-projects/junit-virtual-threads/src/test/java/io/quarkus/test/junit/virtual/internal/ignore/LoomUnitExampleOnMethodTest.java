package io.quarkus.test.junit5.virtual.internal.ignore;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.ShouldPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;
import io.quarkus.test.junit5.virtual.internal.TestPinJfrEvent;

@VirtualThreadUnit
public class LoomUnitExampleOnMethodTest {

    @Test
    @ShouldNotPin
    void failWhenShouldNotPinAndPinDetected() {
        TestPinJfrEvent.pin();
    }

    @Test
    @ShouldNotPin(atMost = 1)
    void failWhenShouldNotPinAtMostAndTooManyPinDetected() {
        TestPinJfrEvent.pin();
        TestPinJfrEvent.pin();
    }

    @Test
    @ShouldPin
    void failWhenMethodShouldPinButNoPinDetected() {
    }

}
