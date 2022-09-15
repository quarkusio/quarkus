package io.quarkus.arc.test.observers.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AsyncObserverTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringProducer.class, StringObserver.class,
            ThreadNameProvider.class);

    @Test
    public void testAsyncObservers() throws InterruptedException, ExecutionException, TimeoutException {
        ArcContainer container = Arc.container();
        StringProducer producer = container.instance(StringProducer.class).get();
        StringObserver observer = container.instance(StringObserver.class).get();
        String currentThread = Thread.currentThread().getName();

        producer.produce("ping");
        List<String> events = observer.getEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0).startsWith("sync::ping"));
        assertTrue(events.get(0).endsWith(currentThread));

        events.clear();

        CompletionStage<String> completionStage = producer.produceAsync("pong");
        assertEquals("pong", completionStage.toCompletableFuture().get(10, TimeUnit.SECONDS));
        assertEquals(1, events.size());
        assertTrue(events.get(0).startsWith("async::pong"));
        assertFalse(events.get(0).endsWith(currentThread));
    }

    @Singleton
    static class StringObserver {

        private List<String> events;

        @Inject
        ThreadNameProvider threadNameProvider;

        @PostConstruct
        void init() {
            events = new CopyOnWriteArrayList<>();
        }

        void observeAsync(@ObservesAsync String value) {
            events.add("async::" + value + "::" + threadNameProvider.get());
        }

        void observeSync(@Observes String value) {
            events.add("sync::" + value + "::" + Thread.currentThread().getName());
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

        CompletionStage<String> produceAsync(String value) {
            return event.fireAsync(value);
        }

    }

    @RequestScoped
    static class ThreadNameProvider {

        String get() {
            return Thread.currentThread().getName();
        }

    }

}
