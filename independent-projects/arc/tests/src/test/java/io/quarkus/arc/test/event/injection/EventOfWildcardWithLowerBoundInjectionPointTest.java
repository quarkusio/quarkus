package io.quarkus.arc.test.event.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.test.ArcTestContainer;

public class EventOfWildcardWithLowerBoundInjectionPointTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyObserver.class);

    @Test
    public void test() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        bean.fire();
        assertEquals(List.of("foo", "bar"), MyObserver.seen);
        bean.selectAndFire();
        assertEquals(List.of("baz", "qux"), MyObserver.seen);
    }

    @Singleton
    @Unremovable
    static class MyBean {
        @Inject
        Event<? super Collection<String>> event;

        public void fire() {
            event.fire(List.of("foo", "bar"));
        }

        public void selectAndFire() {
            event.select(new TypeLiteral<List<String>>() {
            }).fire(List.of("baz", "qux"));
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
