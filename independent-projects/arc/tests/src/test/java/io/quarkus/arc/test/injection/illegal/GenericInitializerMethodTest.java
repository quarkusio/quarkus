package io.quarkus.arc.test.injection.illegal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class GenericInitializerMethodTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Head.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable failure = container.getFailure();
        assertNotNull(failure);
        assertTrue(failure instanceof DefinitionException);
        assertTrue(failure.getMessage().contains("Initializer method may not be generic (declare type parameters)"));
    }

    @Dependent
    static class Head {
        @Inject
        <T> void inject() {
        }
    }
}
