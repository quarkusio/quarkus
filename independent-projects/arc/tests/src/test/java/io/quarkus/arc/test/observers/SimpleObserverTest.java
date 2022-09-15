package io.quarkus.arc.test.observers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SimpleObserverTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringProducer.class, StringObserver.class);

    @Test
    public void testObserver() {
        StringProducer producer = Arc.container().instance(StringProducer.class).get();
        StringObserver observer = Arc.container().instance(StringObserver.class).get();
        producer.produce("foo");
        producer.produce("ping");
        List<String> events = observer.getEvents();
        assertEquals(2, events.size());
    }

    @Singleton
    static class StringObserver {

        private List<String> events;

        @PostConstruct
        void init() {
            events = new CopyOnWriteArrayList<>();
        }

        void observeString(@Observes String value) {
            events.add(value);
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
