package io.quarkus.arc.test.event.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class InstanceOfWildcardWithUpperBoundOfEventOfWildcardWithLowerBoundInjectionPointTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyObserver.class);

    @Test
    public void test() {
        Arc.container().instance(MyBean.class).get().fire();
        assertEquals(List.of("foo", "bar"), MyObserver.seen);
    }

    @Dependent
    public static class MyBean {
        @Inject
        Instance<? extends Event<? super Collection<String>>> event;

        public void fire() {
            event.get().fire(List.of("foo", "bar"));
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
