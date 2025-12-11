package io.quarkus.test.junit.virtual.internal.ignore;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.ShouldPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;
import io.quarkus.test.junit.virtual.internal.TestPinJfrEvent;

@VirtualThreadUnit
@ShouldNotPin // You can use @ShouldNotPin or @ShouldPin on the class itself, it's applied to each method.
public class LoomUnitExampleShouldNotPinOnClassTest {

    @Test
    public void failWhenShouldNotPinAndPinDetected() {
        TestPinJfrEvent.pin();
    }

    @Test
    @ShouldPin(atMost = 1)
    public void overrideClassAnnotation() {
        TestPinJfrEvent.pin();
    }

    @Test
    @ShouldPin
    public void failWhenMethodShouldPinButNoPinDetected() {

    }

}
