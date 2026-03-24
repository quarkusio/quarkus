package io.quarkus.vertx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.EventBus;

public class MessageConsumerReturnTypesTest {

    private static final String NULL_BODY = "NULL_BODY";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(ReturnTypeConsumers.class));

    @Inject
    EventBus eventBus;

    private Object requestAndAwait(String address, Object message, boolean nullAsMarker) throws InterruptedException {
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.request(address, message, ar -> {
            if (ar.succeeded()) {
                Object body = ar.result().body();
                synchronizer.offer(nullAsMarker && body == null ? NULL_BODY : body);
            } else {
                synchronizer.offer(ar.cause());
            }
        });
        return synchronizer.poll(5, TimeUnit.SECONDS);
    }

    @Test
    public void testUniVoidReturn() throws InterruptedException {
        assertEquals(NULL_BODY, requestAndAwait("uni-void", "hello", true));
    }

    @Test
    public void testNullReturnFromStringMethod() throws InterruptedException {
        // When the return value is null, no reply is sent by the invoker.
        // The request times out because no reply is ever produced.
        // This documents the current behavior; it may be revisited in Quarkus 4.
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.request("null-return", "hello", ar -> {
            if (ar.succeeded()) {
                synchronizer.offer(ar.result().body() == null ? NULL_BODY : ar.result().body());
            } else {
                synchronizer.offer("NO_REPLY");
            }
        });
        assertNull(synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testUniNullReturn() throws InterruptedException {
        assertEquals(NULL_BODY, requestAndAwait("uni-null", "hello", true));
    }

    @Test
    public void testVoidReturnWithMessageParam() throws InterruptedException {
        assertEquals("HELLO", requestAndAwait("void-message-reply", "hello", false));
    }

    @Test
    public void testIntegerReturn() throws InterruptedException {
        assertEquals(42, requestAndAwait("integer-return", 21, false));
    }

    @Test
    public void testBooleanReturn() throws InterruptedException {
        assertEquals(true, requestAndAwait("boolean-return", "check", false));
    }

    static class ReturnTypeConsumers {

        @ConsumeEvent("uni-void")
        Uni<Void> uniVoid(String msg) {
            return Uni.createFrom().voidItem();
        }

        @ConsumeEvent("null-return")
        String nullReturn(String msg) {
            return null;
        }

        @ConsumeEvent("uni-null")
        Uni<String> uniNull(String msg) {
            return Uni.createFrom().nullItem();
        }

        @ConsumeEvent("void-message-reply")
        void voidMessageReply(io.vertx.core.eventbus.Message<String> msg) {
            msg.reply(msg.body().toUpperCase());
        }

        @ConsumeEvent("integer-return")
        int integerReturn(int value) {
            return value * 2;
        }

        @ConsumeEvent("boolean-return")
        boolean booleanReturn(String msg) {
            return true;
        }
    }
}
