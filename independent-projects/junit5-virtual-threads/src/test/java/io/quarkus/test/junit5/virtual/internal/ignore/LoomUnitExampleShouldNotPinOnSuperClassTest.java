package io.quarkus.test.junit5.virtual.internal.ignore;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit5.virtual.ShouldPin;
import io.quarkus.test.junit5.virtual.internal.TestPinJfrEvent;

public class LoomUnitExampleShouldNotPinOnSuperClassTest extends LoomUnitExampleShouldNotPinOnSuperClass {

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
    @ShouldPin // Method annotation overrides the class annotation
    public void failWhenShouldPinAndNoPinDetected() {

    }

}
