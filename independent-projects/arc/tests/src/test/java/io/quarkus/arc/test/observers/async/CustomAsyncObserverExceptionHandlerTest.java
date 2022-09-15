package io.quarkus.arc.test.observers.async;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.AsyncObserverExceptionHandler;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CustomAsyncObserverExceptionHandlerTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringProducer.class, StringObserver.class,
            MyAsyncObserverExceptionHandler.class);

    @Test
    public void testAsyncObservers() throws InterruptedException, ExecutionException, TimeoutException {
        StringProducer producer = Arc.container().instance(StringProducer.class).get();

        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        producer.produceAsync("pong").exceptionally(ex -> {
            synchronizer.add(ex);
            return ex.getMessage();
        });

        Object exception = synchronizer.poll(10, TimeUnit.SECONDS);
        assertNotNull(exception);
        assertTrue(exception instanceof RuntimeException);
        assertTrue(MyAsyncObserverExceptionHandler.HANDLED.get());
    }

    @Singleton
    static class StringObserver {

        void observeStr(@ObservesAsync String value) {
            throw new RuntimeException("nok");
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

    @Singleton
    static class MyAsyncObserverExceptionHandler implements AsyncObserverExceptionHandler {

        static final AtomicBoolean HANDLED = new AtomicBoolean(false);

        @Override
        public void handle(Throwable throwable, ObserverMethod<?> observerMethod, EventContext<?> eventContext) {
            HANDLED.set(true);
        }

    }

}
