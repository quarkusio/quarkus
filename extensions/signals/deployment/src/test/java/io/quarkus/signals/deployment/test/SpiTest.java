package io.quarkus.signals.deployment.test;

import static io.quarkus.signals.spi.ReceiverInterceptor.ID_REQUEST_CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.ComponentOrder;
import io.quarkus.signals.spi.ReceiverInterceptor;
import io.quarkus.signals.spi.SignalMetadataEnricher;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

/**
 * Tests {@link SignalMetadataEnricher} and {@link ReceiverInterceptor} SPI.
 */
public class SpiTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    Cmd.class, MyReceivers.class, RequestIdentity.class,
                    TimestampEnricher.class, CorrelationIdEnricher.class,
                    LoggingInterceptor.class, TransformInterceptor.class));

    @Inject
    Signal<Cmd> signal;

    @Inject
    MyReceivers receivers;

    @Inject
    @Any
    LoggingInterceptor loggingInterceptor;

    @Inject
    @Any
    TransformInterceptor transformInterceptor;

    @Test
    public void testMetadataEnrichers() {
        receivers.receivedContexts.clear();

        String result = signal.reactive().request(new Cmd("hello"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertNotNull(result);
        // Verify both enrichers added metadata
        assertEquals(1, receivers.receivedContexts.size());
        SignalContext<?> ctx = receivers.receivedContexts.get(0);
        assertNotNull(ctx.metadata().get("timestamp"));
        assertNotNull(ctx.metadata().get("correlationId"));
        assertTrue(ctx.metadata().get("correlationId").toString().startsWith("corr-"));
    }

    @Inject
    @Any
    TimestampEnricher timestampEnricher;

    @Test
    public void testEnricherOrdering() {
        // CorrelationIdEnricher runs before TimestampEnricher (@ComponentOrder)
        // TimestampEnricher sees correlationId in signal context because a new
        // SignalContext is created after each enricher run
        receivers.receivedContexts.clear();

        signal.reactive().request(new Cmd("order"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertTrue(timestampEnricher.correlationIdPresentAtEnrichTime,
                "TimestampEnricher must see correlationId set by the earlier CorrelationIdEnricher");
    }

    @Test
    public void testEnricherPutMetadataThrowsOnDuplicateKey() {
        // Signal already has "correlationId" in metadata — enricher tries to put the same key
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            signal.putMetadata("correlationId", "existing")
                    .reactive().request(new Cmd("dup"), String.class)
                    .ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
        });
        assertTrue(e.getMessage().contains("correlationId"));
    }

    @Test
    public void testInterceptorTransformsResult() {
        loggingInterceptor.log.clear();

        // LoggingInterceptor
        // TransformInterceptor uppercases String results
        String result = signal.reactive().request(new Cmd("hello"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertEquals(1, loggingInterceptor.log.size());
        assertTrue(loggingInterceptor.log.get(0).contains("Cmd"));
        // Receiver returns "hello", TransformInterceptor uppercases it
        assertEquals("HELLO", result);
    }

    @Test
    public void testInterceptorCalledPerReceiver() {
        loggingInterceptor.log.clear();

        // publish delivers to all receivers — interceptor should be called for each
        signal.reactive().publish(new Cmd("multi"))
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertEquals(2, loggingInterceptor.log.size());
    }

    @Test
    public void testInterceptorSharesRequestContextWithReceiver() {
        loggingInterceptor.interceptorRequestIds.clear();
        transformInterceptor.interceptorRequestIds.clear();
        receivers.receiverRequestIds.clear();

        signal.reactive().request(new Cmd("ctx-test"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertEquals(1, loggingInterceptor.interceptorRequestIds.size());
        assertEquals(1, transformInterceptor.interceptorRequestIds.size());
        assertEquals(1, receivers.receiverRequestIds.size());
        int receiverRequestId = receivers.receiverRequestIds.get(0);
        assertEquals(receiverRequestId, loggingInterceptor.interceptorRequestIds.get(0),
                "LoggingInterceptor and receiver must share the same CDI request context");
        assertEquals(receiverRequestId, transformInterceptor.interceptorRequestIds.get(0),
                "TransformInterceptor and receiver must share the same CDI request context");
    }

    // --- Signal types ---

    record Cmd(String value) {
    }

    // --- Receivers ---

    @Singleton
    public static class MyReceivers {

        final List<SignalContext<?>> receivedContexts = new CopyOnWriteArrayList<>();
        final List<Integer> receiverRequestIds = new CopyOnWriteArrayList<>();

        @Inject
        RequestIdentity requestIdentity;

        String process(@Receives SignalContext<Cmd> ctx) {
            receivedContexts.add(ctx);
            receiverRequestIds.add(requestIdentity.getId());
            return ctx.signal().value();
        }

        void observe(@Receives Cmd cmd) {
        }
    }

    // --- Enrichers ---

    @Identifier("correlation-id")
    @ComponentOrder(before = "timestamp")
    @Singleton
    public static class CorrelationIdEnricher implements SignalMetadataEnricher {

        @Override
        public void enrich(EnrichmentContext context) {
            context.putMetadata("correlationId", "corr-" + System.nanoTime());
        }
    }

    @Identifier("timestamp")
    @Singleton
    public static class TimestampEnricher implements SignalMetadataEnricher {

        boolean correlationIdPresentAtEnrichTime;

        @Override
        public void enrich(EnrichmentContext context) {
            context.putMetadata("timestamp", System.currentTimeMillis());
            correlationIdPresentAtEnrichTime = context.signalContext().metadata().containsKey("correlationId");
        }
    }

    // --- Interceptors ---

    @Identifier("logging")
    @ComponentOrder(before = "transform", after = ID_REQUEST_CONTEXT)
    @Singleton
    public static class LoggingInterceptor implements ReceiverInterceptor {

        final List<String> log = new CopyOnWriteArrayList<>();
        final List<Integer> interceptorRequestIds = new CopyOnWriteArrayList<>();

        @Inject
        RequestIdentity requestIdentity;

        @Override
        public Uni<Object> intercept(InterceptionContext context) {
            log.add("invoke:" + context.signalContext().signalType());
            interceptorRequestIds.add(requestIdentity.getId());
            return context.proceed();
        }
    }

    @Identifier("transform")
    @ComponentOrder(after = ID_REQUEST_CONTEXT)
    @Singleton
    public static class TransformInterceptor implements ReceiverInterceptor {

        final List<Integer> interceptorRequestIds = new CopyOnWriteArrayList<>();

        @Inject
        RequestIdentity requestIdentity;

        @Override
        public Uni<Object> intercept(InterceptionContext context) {
            interceptorRequestIds.add(requestIdentity.getId());
            return context.proceed().onItem().transform(result -> {
                if (result instanceof String s) {
                    return s.toUpperCase();
                }
                return result;
            });
        }
    }

    // --- Request-scoped bean for verifying context sharing ---

    @RequestScoped
    public static class RequestIdentity {

        private int id;

        @PostConstruct
        void init() {
            id = ThreadLocalRandom.current().nextInt();
        }

        public int getId() {
            return id;
        }
    }
}
