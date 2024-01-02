package io.quarkus.smallrye.reactivemessaging.channels;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EmitterWithMultipleInjectionPointsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ChannelEmitterWithMultipleDefinitions.class))
            .setExpectedException(DeploymentException.class);

    @Inject
    ChannelEmitterWithOverflow bean;

    @Test
    public void testEmitter() {
        fail();
    }

}
