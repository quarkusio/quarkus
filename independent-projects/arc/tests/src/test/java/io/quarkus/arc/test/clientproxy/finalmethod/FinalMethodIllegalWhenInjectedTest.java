package io.quarkus.arc.test.clientproxy.finalmethod;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class FinalMethodIllegalWhenInjectedTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Moo.class, MooConsumer.class)
            .strictCompatibility(true)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DeploymentException.class, error);
        assertTrue(error.getMessage().contains("must not declare non-static final methods"));
    }

    @ApplicationScoped
    static class Moo {
        final int getVal() {
            return -1;
        }
    }

    // to trigger deployment-time error (in strict compatible mode)
    @Dependent
    static class MooConsumer {
        @Inject
        Moo moo;
    }
}
