package io.quarkus.vertx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.mutiny.core.MultiMap;

public class HeadersMessageConsumerMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> {
                jar.addClasses(VertxMessageConsumers.class);
                jar.addClasses(MutinyMessageConsumers.class);
            });

    @Inject
    VertxMessageConsumers vertxBean;

    @Inject
    EventBus bus;

    @Inject
    MutinyMessageConsumers mutinyBean;

    @Test
    public void testSend() throws InterruptedException {
        // Given
        VertxMessageConsumers.MESSAGES.clear();
        VertxMessageConsumers.latch = new CountDownLatch(1);
        DeliveryOptions options = new DeliveryOptions()
                .addHeader("header", "testSend");

        // When
        bus.send("vertx-headers", "void", options);

        // Then
        VertxMessageConsumers.latch.await(2, TimeUnit.SECONDS);
        assertFalse(VertxMessageConsumers.MESSAGES.isEmpty());
        Map.Entry<io.vertx.core.MultiMap, String> entry = VertxMessageConsumers.MESSAGES.get(0);
        assertEquals("testSend", entry.getKey().get("header"));
        assertEquals("void", entry.getValue());
    }

    @Test
    public void testMutinySend() throws InterruptedException {
        // Given
        MutinyMessageConsumers.MESSAGES.clear();
        MutinyMessageConsumers.latch = new CountDownLatch(1);
        DeliveryOptions options = new DeliveryOptions()
                .addHeader("header", "testMutinySend");

        // When
        bus.send("mutiny-headers", "void", options);

        // Then
        MutinyMessageConsumers.latch.await(2, TimeUnit.SECONDS);
        assertFalse(MutinyMessageConsumers.MESSAGES.isEmpty());
        Map.Entry<MultiMap, String> entry = MutinyMessageConsumers.MESSAGES.get(0);
        assertEquals("testMutinySend", entry.getKey().get("header"));
        assertEquals("void", entry.getValue());
    }

    @Test
    public void testRequest() throws InterruptedException {
        // Given
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        DeliveryOptions options = new DeliveryOptions()
                .addHeader("header", "testRequest");

        // When
        bus.request("vertx-headers-reply", "String", options, ar -> {
            if (ar.succeeded()) {
                try {
                    synchronizer.put(ar.result().body());
                } catch (InterruptedException e) {
                    fail(e);
                }
            } else {
                fail(ar.cause());
            }
        });

        // Then
        assertEquals("testRequest:String", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testMutinyRequest() throws InterruptedException {
        // Given
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        DeliveryOptions options = new DeliveryOptions()
                .addHeader("header", "testMutinyRequest");

        // When
        bus.request("mutiny-headers-reply", "String", options, ar -> {
            if (ar.succeeded()) {
                try {
                    synchronizer.put(ar.result().body());
                } catch (InterruptedException e) {
                    fail(e);
                }
            } else {
                fail(ar.cause());
            }
        });

        // Then
        assertEquals("testMutinyRequest:String", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testRequestUni() throws InterruptedException {
        // Given
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        DeliveryOptions options = new DeliveryOptions()
                .addHeader("header", "testRequestUni");

        // When
        bus.request("vertx-headers-replyUni", "String", options, ar -> {
            if (ar.succeeded()) {
                try {
                    synchronizer.put(ar.result().body());
                } catch (InterruptedException e) {
                    fail(e);
                }
            } else {
                fail(ar.cause());
            }
        });

        // Then
        assertEquals("testRequestUni:String", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testMutinyRequestUni() throws InterruptedException {
        // Given
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        DeliveryOptions options = new DeliveryOptions()
                .addHeader("header", "testMutinyRequestUni");

        // When
        bus.request("mutiny-headers-replyUni", "String", options, ar -> {
            if (ar.succeeded()) {
                try {
                    synchronizer.put(ar.result().body());
                } catch (InterruptedException e) {
                    fail(e);
                }
            } else {
                fail(ar.cause());
            }
        });

        // Then
        assertEquals("testMutinyRequestUni:String", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @ApplicationScoped
    static class VertxMessageConsumers {
        static volatile CountDownLatch latch;
        static final List<Map.Entry<io.vertx.core.MultiMap, String>> MESSAGES = new CopyOnWriteArrayList<>();

        @ConsumeEvent("vertx-headers")
        void consume(io.vertx.core.MultiMap headers, String body) {
            MESSAGES.add(Map.entry(headers, body));
            latch.countDown();
        }

        @ConsumeEvent("vertx-headers-reply")
        String reply(io.vertx.core.MultiMap headers, String body) {
            return headers.get("header") + ":" + body;
        }

        @ConsumeEvent("vertx-headers-replyUni")
        Uni<String> replyUni(io.vertx.core.MultiMap headers, String body) {
            return Uni.createFrom().item(headers.get("header") + ":" + body);
        }
    }

    @ApplicationScoped
    static class MutinyMessageConsumers {
        static volatile CountDownLatch latch;
        static final List<Map.Entry<MultiMap, String>> MESSAGES = new CopyOnWriteArrayList<>();

        @ConsumeEvent("mutiny-headers")
        void consume(MultiMap headers, String body) {
            MESSAGES.add(Map.entry(headers, body));
            latch.countDown();
        }

        @ConsumeEvent("mutiny-headers-reply")
        String reply(MultiMap headers, String body) {
            return headers.get("header") + ":" + body;
        }

        @ConsumeEvent("mutiny-headers-replyUni")
        Uni<String> replyUni(MultiMap headers, String body) {
            return Uni.createFrom().item(headers.get("header") + ":" + body);
        }
    }
}
