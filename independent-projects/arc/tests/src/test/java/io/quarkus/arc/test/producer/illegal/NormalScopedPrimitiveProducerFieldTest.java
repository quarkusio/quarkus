package io.quarkus.arc.test.producer.illegal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class NormalScopedPrimitiveProducerFieldTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Producer.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DeploymentException.class, error);
        assertTrue(error.getMessage().contains("Producer field for a normal scoped bean must not have a primitive type"));
    }

    @Dependent
    static class Producer {
        @Produces
        @ApplicationScoped
        int produce = 0;
    }
}
