package io.quarkus.test.junit5.virtual.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.ShouldPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;

@VirtualThreadUnit
@ShouldNotPin // You can use @ShouldNotPin or @ShouldPin on the class itself, it's applied to each method.
public class LoomUnitExampleOnClassTest {

    @Test
    public void testThatShouldNotPin() {
        // ...
    }

    @Test
    @ShouldPin(atMost = 1) // Method annotation overrides the class annotation
    @EnabledForJreRange(min = JRE.JAVA_21)
    public void testThatShouldPinAtMostOnce() {
        TestPinJfrEvent.pin();
    }

    @Test
    @ShouldNotPin(atMost = 1) // Method annotation overrides the class annotation
    public void testThatShouldNotPinAtMostOnce() {
        TestPinJfrEvent.pin();
    }

}
