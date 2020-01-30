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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.Context;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class MessageConsumerMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SimpleBean.class, Transformer.class));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testSend() throws InterruptedException {
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
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
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
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
    public void testSendDefaultAddress() throws InterruptedException {
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
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
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
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
    public void testPublish() throws InterruptedException {
        SimpleBean.MESSAGES.clear();
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
        SimpleBean.latch = new CountDownLatch(2);
        eventBus.publish("pub", "Hello");
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertTrue(SimpleBean.MESSAGES.contains("hello"));
        assertTrue(SimpleBean.MESSAGES.contains("HELLO"));
    }

    @Test
    public void testBlockingConsumer() throws InterruptedException {
        SimpleBean.MESSAGES.clear();
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
        SimpleBean.latch = new CountDownLatch(1);
        eventBus.publish("blocking", "Hello");
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertEquals(1, SimpleBean.MESSAGES.size());
        String message = SimpleBean.MESSAGES.get(0);
        assertTrue(message.contains("hello::true"));
    }

    @Test
    public void testPublishRx() throws InterruptedException {
        SimpleBean.MESSAGES.clear();
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
        SimpleBean.latch = new CountDownLatch(1);
        eventBus.publish("pub-rx", "Hello");
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertTrue(SimpleBean.MESSAGES.contains("HELLO"));
    }

    @Test
    public void testPublishAxle() throws InterruptedException {
        SimpleBean.MESSAGES.clear();
        EventBus eventBus = Arc.container().instance(EventBus.class).get();
        SimpleBean.latch = new CountDownLatch(1);
        eventBus.publish("pub-axle", "Hello");
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertTrue(SimpleBean.MESSAGES.contains("HELLO"));
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

        @ConsumeEvent(value = "blocking", blocking = true)
        void consumeBlocking(String message) {
            MESSAGES.add(message.toLowerCase() + "::" + Context.isOnWorkerThread());
            latch.countDown();
        }

        @ConsumeEvent("pub-axle")
        void consume(io.vertx.axle.core.eventbus.Message<String> message) {
            MESSAGES.add(message.body().toUpperCase());
            latch.countDown();
        }

        @ConsumeEvent("pub-rx")
        void consume(io.vertx.reactivex.core.eventbus.Message<String> message) {
            MESSAGES.add(message.body().toUpperCase());
            latch.countDown();
        }

        @ConsumeEvent("request")
        String requestContextActive(String message) {
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
