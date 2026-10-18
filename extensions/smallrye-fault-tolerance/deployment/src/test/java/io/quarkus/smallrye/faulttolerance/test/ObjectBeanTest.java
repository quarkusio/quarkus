package io.quarkus.smallrye.faulttolerance.test;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verify that Fault Tolerance does not break if there is a CDI bean without a superclass
 */
public class ObjectBeanTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest();

    @Produces
    @Named("namedObject")
    Object object;

    @Test
    public void verifyFaultToleranceDoesNotBreak() {
        // just make sure that the deployment succeeds
    }

}
