package io.quarkus.arc.test.observers.ordering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ObserverOrderingTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringProducer.class, StringObserver.class);

    @Test
    public void testObservers() {
        ArcContainer container = Arc.container();
        StringProducer producer = container.instance(StringProducer.class).get();
        StringObserver observer = container.instance(StringObserver.class).get();
        producer.produce("sorted");
        List<String> events = observer.getEvents();
        assertEquals(3, events.size());
        assertEquals("1sorted", events.get(0));
        assertEquals("10sorted", events.get(1));
        assertEquals("1000sorted", events.get(2));
    }

    @Singleton
    static class StringObserver {

        private List<String> events;

        @PostConstruct
        void init() {
            events = new CopyOnWriteArrayList<>();
        }

        void observe10(@Observes @Priority(10) String value) {
            events.add("10" + value);
        }

        void observe1(@Observes @Priority(1) String value) {
            events.add("1" + value);
        }

        void observe1000(@Observes @Priority(1000) String value) {
            events.add("1000" + value);
        }

        List<String> getEvents() {
            return events;
        }

    }

    @Dependent
    static class StringProducer {

        @Inject
        Event<String> event;

        void produce(String value) {
            event.fire(value);
        }

    }

}
