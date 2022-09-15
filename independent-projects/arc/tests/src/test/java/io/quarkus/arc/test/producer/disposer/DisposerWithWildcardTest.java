package io.quarkus.arc.test.producer.disposer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DisposerWithWildcardTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Producers.class);

    @Test
    public void testDisposers() {
        ArcContainer container = Arc.container();
        InstanceHandle<Map<String, Long>> instanceA = container.instance(new TypeLiteral<Map<String, Long>>() {
        });
        assertTrue(instanceA.get().containsKey("A"));
        instanceA.destroy();

        InstanceHandle<Map<String, Integer>> instanceB = container.instance(new TypeLiteral<Map<String, Integer>>() {
        });
        assertTrue(instanceB.get().containsKey("B"));
        instanceB.destroy();

        assertEquals(2, Producers.KEYS.size());
    }

    @Singleton
    static class Producers {

        static final List<Object> KEYS = new CopyOnWriteArrayList<>();

        @Singleton
        @Produces
        Map<String, Long> produceA() {
            return Collections.singletonMap("A", 1l);
        }

        @Singleton
        @Produces
        Map<String, Integer> produceB() {
            return Collections.singletonMap("B", 1);
        }

        void dispose(@Disposes Map<?, ?> myMap) {
            KEYS.addAll(myMap.keySet());
        }

    }

}
