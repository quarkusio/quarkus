package io.quarkus.arc.test.producer.generic;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.Optional;
import java.util.function.Function;
import javax.enterprise.inject.Produces;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class GenericProducerHierarchyTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Producer.class);

    @SuppressWarnings("serial")
    @Test
    public void testPrimitiveProducers() {
        InstanceHandle<Function<Optional<String>, String>> strHandle = Arc.container()
                .instance(new TypeLiteral<Function<Optional<String>, String>>() {
                });
        Function<Optional<String>, String> producedStr = strHandle.get();
        assertEquals("foo", producedStr.apply(Optional.of("FOO")));

        InstanceHandle<Function<Optional<Long>, Long>> longHandle = Arc.container()
                .instance(new TypeLiteral<Function<Optional<Long>, Long>>() {
                });
        Function<Optional<Long>, Long> producedLong = longHandle.get();
        assertEquals(Long.valueOf(-10), producedLong.apply(Optional.of(10l)));
    }

    @Singleton
    static class Producer {

        @Produces
        public Produced<String, String> produce() {
            return s -> s.get().toLowerCase();
        }

        @Produces
        public Produced<Long, Long> produceLong() {
            return v -> -v.get();
        }
    }

    static interface Produced<T, R> extends Function<Optional<T>, R> {

    }

}
