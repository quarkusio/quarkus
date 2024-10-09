package io.quarkus.arc.test.sealed;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class SingletonSealedProducerTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Producer.class);

    @Test
    public void test() {
        assertNotNull(Arc.container().select(MySealed.class).get());
    }

    @Dependent
    static class Producer {
        @Produces
        @Singleton
        MySealed produce() {
            return new MySealed();
        }
    }

    static sealed class MySealed permits MySealedSubclass {
    }

    static final class MySealedSubclass extends MySealed {
    }
}
