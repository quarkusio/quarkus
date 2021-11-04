package io.quarkus.arc.test.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AsyncObserverTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(StringProducer.class, StringObserver.class,
                            ThreadNameProvider.class));

    @Inject
    StringProducer producer;

    @Inject
    StringObserver observer;

    @Test
    public void testAsyncObservers() throws InterruptedException, ExecutionException, TimeoutException {
        String currentThread = Thread.currentThread().getName();

        producer.fire("ping");
        List<String> events = observer.getEvents();

        assertEquals(1, events.size());
        assertTrue(events.get(0).startsWith("sync::ping"));
        assertTrue(events.get(0).endsWith(currentThread));

        events.clear();

        CompletionStage<String> completionStage = producer.fireAsync("pong");
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

        void fire(String value) {
            event.fire(value);
        }

        CompletionStage<String> fireAsync(String value) {
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
