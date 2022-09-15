package io.quarkus.arc.test.producer.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AsyncProducerTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(LongProducer.class, LongClient.class);

    @Test
    public void testAsyncProducer() throws InterruptedException, ExecutionException {
        LongProducer.reset();
        LongClient longClient = Arc.container().instance(LongClient.class).get();
        AtomicReference<Long> val = new AtomicReference<>();
        longClient.completionStage.thenAccept(l -> {
            val.set(l);
        });

        assertNull(val.get());

        LongProducer.complete(10);

        assertEquals(Long.valueOf(10), val.get());
        assertEquals(Long.valueOf(10), longClient.completionStage.toCompletableFuture().get());
    }

    @Dependent
    static class LongClient {

        @Inject
        CompletionStage<Long> completionStage;

    }

    @Singleton
    static class LongProducer {

        private static CompletableFuture<Long> future;

        static void complete(long code) {
            future.complete(code);
        }

        static void reset() {
            future = new CompletableFuture<>();
        }

        @Produces
        CompletionStage<Long> produceLong() {
            return future;
        }

    }

}
