package io.quarkus.arc.test.cdiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class CDIProviderTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Moo.class);

    @Test
    public void testProducer() throws IOException {
        Moo moo = CDI.current()
                .select(Moo.class)
                .get();
        assertEquals(10, moo.getVal());
    }

    @AfterAll
    public static void unset() {
        assertTrue(Moo.DESTROYED.get());

        abstract class CDIAccess<T> extends CDI<T> {
            static void reset() {
                providerState.set(initialState());
            }
        }

        CDIAccess.reset();
    }

    @Dependent
    static class Moo {

        private int val;

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        @PostConstruct
        void init() {
            val = 10;
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

        int getVal() {
            return val;
        }

    }

}
