package org.jboss.protean.arc.test.observers.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class AsyncObserverExceptionTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(StringProducer.class, StringObserver.class);

    @Test
    public void testAsyncObservers() throws InterruptedException, ExecutionException, TimeoutException {
        ArcContainer container = Arc.container();
        StringProducer producer = container.instance(StringProducer.class).get();
        StringObserver observer = container.instance(StringObserver.class).get();

        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        producer.produceAsync("pong").exceptionally(ex -> {
            synchronizer.add(ex);
            return ex.getMessage();
        });

        Object exception = synchronizer.poll(10, TimeUnit.SECONDS);
        assertNotNull(exception);
        assertTrue(exception instanceof RuntimeException);

        List<String> events = observer.getEvents();
        assertEquals(2, events.size());
        assertEquals("async1::pong", events.get(0));
        assertEquals("async2::pong", events.get(1));
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
