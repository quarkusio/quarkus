package io.quarkus.test.junit5.virtual.internal.ignore;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.ShouldPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;
import io.quarkus.test.junit5.virtual.internal.TestPinJfrEvent;

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
