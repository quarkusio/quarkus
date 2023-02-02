package io.quarkus.arc.test.observers.ifexists;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class DependentReceptionIfExistsTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(DependentObserver.class)
            .shouldFail()
            .build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
        assertTrue(error.getMessage().contains("@Dependent bean must not have a conditional observer method"));
    }

    @Dependent
    static class DependentObserver {
        void observeString(@Observes(notifyObserver = Reception.IF_EXISTS) String value) {
        }
    }
}
