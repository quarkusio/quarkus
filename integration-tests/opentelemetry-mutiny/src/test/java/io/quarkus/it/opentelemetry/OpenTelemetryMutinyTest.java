package io.quarkus.it.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.tuples.Tuple3;
import io.smallrye.reactive.messaging.providers.helpers.DefaultKeyedMulti;

@QuarkusTest
public class OpenTelemetryMutinyTest {
    private static final Logger Log = Logger.getLogger(OpenTelemetryMutinyTest.class);

    @Inject
    TestProducer testProducer;

    @Inject
    TestReceiver testReceiver;

    @Test
    void multiGroupTest() {
        testStreamTracing(testProducer::emitItem2, testReceiver.receive());
    }

    void testStreamTracing(Consumer<String> emitItem, Multi<Tuple3<String, String, String>> processedItems) {

        // Use a thread-safe list since Multi might emit on different threads
        List<Tuple3<String, String, String>> receivedItems = new CopyOnWriteArrayList<>();

        processedItems.subscribe().with(item -> {
            Log.infof("Test subscriber received item: %s", item);
            receivedItems.add(item);
        });

        // Trigger the stream
        emitItem.accept("a1");
        emitItem.accept("a2");

        // Use Awaitility to replace 'blockingEventually'
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(receivedItems).hasSize(2);
        });

        // Validate trace IDs match
        for (Tuple3<String, String, String> triple : receivedItems) {
            String originalTraceId = triple.getItem2();
            String currentTraceId = triple.getItem3();

            assertThat(currentTraceId)
                    .as("Trace ID should match the original context for item %s", triple.getItem1())
                    .isEqualTo(originalTraceId);
        }
    }

    @ApplicationScoped
    static class TestProducer {
        @Channel("items2")
        Emitter<String> emitter2;

        @WithSpan
        public void emitItem2(String item) {
            emitter2.send(item);
        }
    }

    @ApplicationScoped
    static class TestReceiver{
        @Channel("processed-keyedMulti-items")
        Multi<Tuple3<String, String, String>> processedItems;


        public Multi<Tuple3<String, String, String>> receive() {
            return processedItems;
        }
    }

    @ApplicationScoped
    public static class ItemProcessor {
        private static final Logger Log = Logger.getLogger(ItemProcessor.class);

        @Incoming("items2")
        @Outgoing("processed-keyedMulti-items")
        public Multi<Tuple3<String, String, String>> processItemsByKey(Multi<String> items) {
            return items
                    .map(item -> {
                        Log.infof("Processing item: %s", item);
                        String traceId = Span.fromContext(Context.current()).getSpanContext().getTraceId();
                        // Kotlin's 'to' creates a Pair; Java uses Tuple2
                        return Tuple2.of(item, traceId);
                    })
                    .group().by(tuple -> tuple.getItem1().substring(0, 1)) // Accessing the first char
                    .map(grouped -> new DefaultKeyedMulti<>(grouped.key(), grouped))
                    .flatMap(keyedStream -> {
                        Log.infof("Processing keyedStream for key %s", keyedStream.key());
                        return keyedStream
                                .onItem().transform(tuple -> {
                                    String item = tuple.getItem1();
                                    String originalTraceId = tuple.getItem2();

                                    // Note: The tracing issue you mentioned often occurs in reactive streams
                                    // because Context isn't automatically propagated through flatMap/grouping.
                                    Log.infof("Keyed processing item: %s with traceId: %s", item, originalTraceId);
                                    String currentTraceId = Span.fromContext(Context.current()).getSpanContext().getTraceId();

                                    return Tuple3.of(item, originalTraceId, currentTraceId);
                                });
                    });
        }
    }
}
