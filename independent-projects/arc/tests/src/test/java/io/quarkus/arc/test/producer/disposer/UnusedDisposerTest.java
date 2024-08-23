package io.quarkus.arc.test.producer.disposer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class UnusedDisposerTest {
    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Producer.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains(
                "No producer method or field declared by the bean class that is assignable to the disposed parameter of a disposer method"));
    }

    @Singleton
    static class Producer {
        @Singleton
        @Produces
        String produce() {
            return "produced";
        }

        void disposeString(@Disposes String str) {
        }

        void disposeInteger(@Disposes Integer integer) {
        }
    }
}
