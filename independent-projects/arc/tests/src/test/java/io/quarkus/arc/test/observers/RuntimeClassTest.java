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

public class RuntimeClassTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(NumberProducer.class, NumberObserver.class);

    @Test
    public void testObserver() {
        NumberProducer producer = Arc.container().instance(NumberProducer.class).get();
        NumberObserver observer = Arc.container().instance(NumberObserver.class).get();
        producer.produce(1l);
        producer.produce(.1);
        List<Number> numbers = observer.getNumbers();
        assertEquals(2, numbers.size());
        assertEquals(1l, numbers.get(0));
    }

    @Singleton
    static class NumberObserver {

        private List<Number> numbers;

        @PostConstruct
        void init() {
            numbers = new CopyOnWriteArrayList<>();
        }

        void observeLong(@Observes Long value) {
            numbers.add(value);
        }

        void observeDouble(@Observes Double value) {
            numbers.add(value);
        }

        List<Number> getNumbers() {
            return numbers;
        }

    }

    @Dependent
    static class NumberProducer {

        @Inject
        Event<Number> event;

        void produce(Number value) {
            event.fire(value);
        }

    }

}
