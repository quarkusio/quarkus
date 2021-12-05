package io.quarkus.vertx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class MessageConsumerMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SimpleBean.class, Transformer.class));

    @Inject
    SimpleBean simpleBean;

    @Inject
    EventBus eventBus;

    @Test
    public void testSend() throws InterruptedException {
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.request("foo", "hello", ar -> {
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
        assertEquals("HELLO", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testSendAsync() throws InterruptedException {
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.request("foo-async", "hello", ar -> {
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
        assertEquals("olleh", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testSendAsyncUni() throws InterruptedException {
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.request("foo-async-uni", "hello-uni", ar -> {
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
        assertEquals("inu-olleh", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testSendDefaultAddress() throws InterruptedException {
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.request("io.quarkus.vertx.deployment.MessageConsumerMethodTest$SimpleBean", "Hello", ar -> {
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
        assertEquals("hello", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testRequestContext() throws InterruptedException {
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.request("request", "Martin", ar -> {
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
        assertEquals("MArtin", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testBlockingRequestContext() throws InterruptedException {
        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();
        eventBus.request("blocking-request", "Lu", ar -> {
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
        assertEquals("Lu", synchronizer.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testPublish() throws InterruptedException {
        SimpleBean.MESSAGES.clear();
        SimpleBean.latch = new CountDownLatch(2);
        eventBus.publish("pub", "Hello");
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertTrue(SimpleBean.MESSAGES.contains("hello"));
        assertTrue(SimpleBean.MESSAGES.contains("HELLO"));
    }

    @Test
    public void testBlockingConsumer() throws InterruptedException {
        SimpleBean.MESSAGES.clear();
        SimpleBean.latch = new CountDownLatch(1);
        eventBus.publish("blocking", "Hello");
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertEquals(1, SimpleBean.MESSAGES.size());
        String message = SimpleBean.MESSAGES.get(0);
        assertTrue(message.contains("hello::true"));
    }

    @Test
    public void testPublishMutiny() throws InterruptedException {
        SimpleBean.MESSAGES.clear();
        SimpleBean.latch = new CountDownLatch(1);
        eventBus.publish("pub-mutiny", "Hello");
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertTrue(SimpleBean.MESSAGES.contains("HELLO"));
    }

    @Test
    public void testBlockingConsumerUsingSmallRyeBlocking() throws InterruptedException {
        SimpleBean.MESSAGES.clear();
        SimpleBean.latch = new CountDownLatch(1);
        eventBus.publish("worker", "Hello");
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertEquals(1, SimpleBean.MESSAGES.size());
        String message = SimpleBean.MESSAGES.get(0);
        assertTrue(message.contains("hello::true"));
    }

    static class SimpleBean {

        static volatile CountDownLatch latch;

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        @Inject
        Transformer transformer;

        @ConsumeEvent // io.quarkus.vertx.deployment.MessageConsumerMethodTest$SimpleBean
        String sendDefaultAddress(String message) {
            return message.toLowerCase();
        }

        @ConsumeEvent("foo")
        String reply(String message) {
            return message.toUpperCase();
        }

        @ConsumeEvent("pub")
        void consume(String message) {
            MESSAGES.add(message.toLowerCase());
            latch.countDown();
        }

        @ConsumeEvent("pub")
        void consume(Message<String> message) {
            MESSAGES.add(message.body().toUpperCase());
            latch.countDown();
        }

        @ConsumeEvent("foo-async")
        CompletionStage<String> replyAsync(String message) {
            return CompletableFuture.completedFuture(new StringBuilder(message).reverse().toString());
        }

        @ConsumeEvent("foo-async-uni")
        Uni<String> replyAsyncUni(String message) {
            return Uni.createFrom().item(new StringBuilder(message).reverse().toString());
        }

        @ConsumeEvent(value = "blocking", blocking = true)
        void consumeBlocking(String message) {
            MESSAGES.add(message.toLowerCase() + "::" + Context.isOnWorkerThread());
            latch.countDown();
        }

        @ConsumeEvent("pub-mutiny")
        void consume(io.vertx.mutiny.core.eventbus.Message<String> message) {
            MESSAGES.add(message.body().toUpperCase());
            latch.countDown();
        }

        @ConsumeEvent("request")
        String requestContextActive(String message) {
            return transformer.transform(message);
        }

        @ConsumeEvent(value = "worker")
        @Blocking
        void consumeBlockingUsingRunOnWorkerThread(String message) {
            MESSAGES.add(message.toLowerCase() + "::" + Context.isOnWorkerThread());
            latch.countDown();
        }

        @Blocking
        @ConsumeEvent("blocking-request")
        String blockingRequestContextActive(String message) {
            return transformer.transform(message);
        }
    }

    @RequestScoped
    static class Transformer {

        String transform(String message) {
            return message.replace('a', 'A');
        }

    }

}
