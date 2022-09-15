package io.quarkus.arc.test.observers.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AsyncObserverExceptionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringProducer.class, StringObserver.class);

    @Test
    public void testAsyncObserversSingleException() throws InterruptedException {
        ArcContainer container = Arc.container();
        StringProducer producer = container.instance(StringProducer.class).get();
        StringObserver observer = container.instance(StringObserver.class).get();

        final List<Throwable> suppressed = new ArrayList<>();
        BlockingQueue<Throwable> synchronizer = new LinkedBlockingQueue<>();
        producer.produceAsync("pong").exceptionally(ex -> {
            Arrays.stream(ex.getSuppressed()).forEach(t -> suppressed.add(t));
            synchronizer.add(ex);
            return ex.getMessage();
        });

        Throwable exception = synchronizer.poll(10, TimeUnit.SECONDS);

        // assert suppressed exception is always present
        assertEquals(1, suppressed.size());
        assertTrue(suppressed.get(0) instanceof RuntimeException);

        // assert actual exception, always a CompletionException
        assertNotNull(exception);
        assertTrue(exception instanceof CompletionException);
        // in case of single exception in event chain, the cause is the exception
        assertTrue(exception.getCause() instanceof RuntimeException);

        List<String> events = observer.getEvents();
        assertEquals(2, events.size());
        assertEquals("async1::pong", events.get(0));
        assertEquals("async2::pong", events.get(1));
    }

    @Test
    public void testAsyncObserversMultipleExceptions() throws InterruptedException {
        ArcContainer container = Arc.container();
        StringProducer producer = container.instance(StringProducer.class).get();
        StringObserver observer = container.instance(StringObserver.class).get();

        final List<Throwable> suppressed = new ArrayList<>();
        BlockingQueue<Throwable> synchronizer = new LinkedBlockingQueue<>();
        producer.produceAsync("ping").exceptionally(ex -> {
            Arrays.stream(ex.getSuppressed()).forEach(t -> suppressed.add(t));
            synchronizer.add(ex);
            return ex.getMessage();
        });

        Throwable exception = synchronizer.poll(10, TimeUnit.SECONDS);

        // assert all suppressed exceptions are present
        assertEquals(2, suppressed.size());
        assertTrue(suppressed.get(0) instanceof RuntimeException);
        assertTrue(suppressed.get(1) instanceof IllegalStateException);

        // assert actual exception, always a CompletionException
        assertNotNull(exception);
        assertTrue(exception instanceof CompletionException);
        // in case of multiple exceptions in event chain, the cause is expected to be null
        assertNull(exception.getCause());

        List<String> events = observer.getEvents();
        assertEquals(3, events.size());
        assertEquals("async1::ping", events.get(0));
        assertEquals("async2::ping", events.get(1));
        assertEquals("async3::ping", events.get(2));
    }

    @Singleton
    static class StringObserver {

        private List<String> events;

        @PostConstruct
        void init() {
            events = new CopyOnWriteArrayList<>();
        }

        void observeAsync1(@ObservesAsync @Priority(1) String value) {
            events.add("async1::" + value);
            throw new RuntimeException("nok");
        }

        void observeAsync2(@ObservesAsync @Priority(2) String value) {
            events.add("async2::" + value);
        }

        void observeAsync3(@ObservesAsync @Priority(3) String value) {
            if (value.equals("ping")) {
                events.add("async3::" + value);
                throw new IllegalStateException("nok");
            }
        }

        List<String> getEvents() {
            return events;
        }

    }

    @Dependent
    static class StringProducer {

        @Inject
        Event<String> event;

        CompletionStage<String> produceAsync(String value) {
            return event.fireAsync(value);
        }

    }

}
