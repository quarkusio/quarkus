package io.quarkus.arc.test.event.lookup;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class EventOfUnboundedWildcardLookupTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class);

    @Test
    public void test() {
        assertThrows(IllegalArgumentException.class, () -> {
            Arc.container().instance(MyBean.class).get().obtainEvent();
        });
    }

    @Dependent
    public static class MyBean {
        @Inject
        Instance<Object> lookup;

        public Event<?> obtainEvent() {
            return lookup.select(new TypeLiteral<Event<?>>() {
            }).get();
        }
    }
}
