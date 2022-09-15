package io.quarkus.arc.test.observers.inheritance.typevariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * https://github.com/quarkusio/quarkus/issues/25364
 */
public class ObserverInheritanceTypeVariableTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyEvent.class, MyAEvent.class, MyBEvent.class, MyAService.class,
            MyBService.class, EventSource.class);

    @Test
    public void testNotification() {
        Arc.container().instance(EventSource.class).get().sendA();
        assertNotNull(MyAService.event);
        assertNull(MyBService.event);
    }

    static class MyEvent {
    }

    static class MyAEvent extends MyEvent {
    }

    static class MyBEvent extends MyEvent {
    }

    static abstract class AbstractService<E extends MyEvent> {

        void onEvent(@Observes E myEvent) {
            doSomething(myEvent);
        }

        abstract void doSomething(E myEvent);
    }

    @ApplicationScoped
    static class MyAService extends AbstractService<MyAEvent> {

        static volatile MyAEvent event;

        @Override
        protected void doSomething(MyAEvent myEvent) {
            MyAService.event = myEvent;
        }
    }

    @ApplicationScoped
    static class MyBService extends AbstractService<MyBEvent> {

        static volatile MyBEvent event;

        @Override
        void doSomething(MyBEvent myEvent) {
            MyBService.event = myEvent;
        }
    }

    @Unremovable
    @Dependent
    static class EventSource {

        @Inject
        Event<MyEvent> event;

        void sendA() {
            event.fire(new MyAEvent());
        }
    }

}
