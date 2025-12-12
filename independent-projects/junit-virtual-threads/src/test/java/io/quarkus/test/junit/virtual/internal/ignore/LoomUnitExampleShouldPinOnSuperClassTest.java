package io.quarkus.test.junit.virtual.internal.ignore;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.internal.TestPinJfrEvent;

public class LoomUnitExampleShouldPinOnSuperClassTest extends LoomUnitExampleShouldPinOnSuperClass {

    @Test
    @ShouldNotPin // Method annotation overrides the class annotation
    public void failWhenShouldNotPinAndPinDetected() {
        TestPinJfrEvent.pin();
    }

    @Test
    public void failWhenShouldPinAndNoPinDetected() {

    }

}
