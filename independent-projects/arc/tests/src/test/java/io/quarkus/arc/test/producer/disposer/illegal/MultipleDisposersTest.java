package io.quarkus.arc.test.producer.disposer.illegal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class MultipleDisposersTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(ProducerDisposer.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("Multiple disposer methods found"));
        assertTrue(error.getMessage().contains("produceString"));
        assertTrue(error.getMessage().contains("disposeString"));
        assertTrue(error.getMessage().contains("disposeCharSequence"));
    }

    @Dependent
    static class ProducerDisposer {
        @Produces
        @Dependent
        String produceString() {
            return "";
        }

        void disposeString(@Disposes String ignored) {
        }

        void disposeCharSequence(@Disposes CharSequence ignored) {
        }
    }
}
