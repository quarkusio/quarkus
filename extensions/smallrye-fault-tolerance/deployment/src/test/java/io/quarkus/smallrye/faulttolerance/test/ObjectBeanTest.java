package io.quarkus.smallrye.faulttolerance.test;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Verify that Fault Tolerance does not break if there is a CDI bean without a superclass
 */
public class ObjectBeanTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Produces
    @Named("namedObject")
    Object object;

    @Test
    public void verifyFaultToleranceDoesNotBreak() {
        // just make sure that the deployment succeeds
    }

}
