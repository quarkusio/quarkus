package io.quarkus.test.junit5.virtual.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.ShouldPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;

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
    @EnabledForJreRange(min = JRE.JAVA_21)
    void methodShouldPin() {
        TestPinJfrEvent.pin();
    }

    @Test
    void methodNoAnnotation() {
    }

}
