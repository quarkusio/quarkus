package io.quarkus.smallrye.reactivemessaging.channels;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class EmitterWithMultipleDifferentInjectionPointsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ChannelEmitterWithMultipleDifferentDefinitions.class))
            .setExpectedException(DeploymentException.class);

    @Inject
    ChannelEmitterWithMultipleDifferentDefinitions bean;

    @Test
    public void testEmitter() {
        fail();
    }

}
