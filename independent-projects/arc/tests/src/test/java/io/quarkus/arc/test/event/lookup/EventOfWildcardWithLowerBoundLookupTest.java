package io.quarkus.arc.test.event.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class EventOfWildcardWithLowerBoundLookupTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyObserver.class);

    @Test
    public void test() {
        Event<? super Collection<String>> event = Arc.container().instance(MyBean.class).get().obtainEvent();
        event.fire(List.of("foo", "bar"));
        assertEquals(List.of("foo", "bar"), MyObserver.seen);
        event.select(new TypeLiteral<List<String>>() {
        }).fire(List.of("baz", "qux"));
        assertEquals(List.of("baz", "qux"), MyObserver.seen);
    }

    @Dependent
    public static class MyBean {
        @Inject
        Instance<Object> lookup;

        public Event<? super Collection<String>> obtainEvent() {
            return lookup.select(new TypeLiteral<Event<? super Collection<String>>>() {
            }).get();
        }
    }

    @Dependent
    public static class MyObserver {
        static List<String> seen;

        public void observe(@Observes List<String> event) {
            seen = event;
        }
    }
}
