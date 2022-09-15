package io.quarkus.arc.test.producer.illegal;

import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.Produces;
import java.io.Serializable;
import java.time.temporal.Temporal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class IllegalProducerTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(IllegalProducer.class);

    @Test
    public void testNormalScopedProducerMethodReturnsNull() {
        Serializable serializable = Arc.container().instance(Serializable.class).get();
        try {
            serializable.toString();
            fail();
        } catch (IllegalProductException expected) {
        }
    }

    @Test
    public void testNormalScopedProducerFieldIsNull() {
        Temporal temporal = Arc.container().instance(Temporal.class).get();
        try {
            temporal.toString();
            fail();
        } catch (IllegalProductException expected) {
        }
    }

    @Dependent
    static class IllegalProducer {

        @Produces
        @ApplicationScoped
        Temporal temporal = null;

        @Produces
        @ApplicationScoped
        Serializable produce() {
            return null;
        }

    }

}
