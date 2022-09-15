package io.quarkus.arc.test.producer.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class GenericProducerHierarchyTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Producer.class, Registry.class);

    @Test
    public void testGenericProducers() {
        Registry registry = Arc.container().instance(Registry.class).get();
        assertEquals("foo", registry.strProducer.apply(s -> s.toLowerCase()).get());
        assertEquals(Long.valueOf(-10), registry.longProducer.apply(l -> Long.valueOf(-l)).get());
    }

    @Singleton
    static class Producer {

        @Produces
        public Produced<String, CharSequence> produce() {
            return f -> Optional.of(f.apply("FOO"));
        }

        @Produces
        public Produced<Integer, Long> produceLong() {
            return f -> Optional.of(f.apply(10));
        }
    }

    static interface Produced<T, R> extends Function<Function<T, R>, Optional<R>> {

    }

    @Singleton
    static class Registry {

        @Inject
        Function<Function<String, CharSequence>, Optional<CharSequence>> strProducer;

        @Inject
        Function<Function<Integer, Long>, Optional<Long>> longProducer;

    }

}
