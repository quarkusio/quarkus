package io.quarkus.vertx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;

/**
 * Verify that event consumer are attached to different contexts.
 */
public class MessageConsumerContextTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(MessageConsumers.class));

    @Inject
    MessageConsumers messageConsumers;

    @Inject
    EventBus eventBus;

    @RepeatedTest(5)
    public void testSend() throws InterruptedException {
        MessageConsumers.MESSAGES.clear();
        MessageConsumers.latch = new CountDownLatch(3);
        eventBus.send("send", "foo");
        eventBus.send("send", "bar");
        eventBus.send("send", "baz");
        assertTrue(MessageConsumers.latch.await(3, TimeUnit.SECONDS));
        if (Runtime.getRuntime().availableProcessors() > 1) {
            assertEquals(3, MessageConsumers.MESSAGES.size());
        } else {
            assertTrue(MessageConsumers.MESSAGES.size() >= 2);
        }
    }

    @RepeatedTest(5)
    public void testPublish() throws InterruptedException {
        MessageConsumers.MESSAGES.clear();
        MessageConsumers.latch = new CountDownLatch(9); // 3 messages x 3 consumers
        eventBus.publish("pub", "foo");
        eventBus.publish("pub", "bar");
        eventBus.publish("pub", "baz");
        assertTrue(MessageConsumers.latch.await(3, TimeUnit.SECONDS));
        if (Runtime.getRuntime().availableProcessors() > 1) {
            // The 2 event loops and additional worker contexts
            assertTrue(MessageConsumers.MESSAGES.size() >= 3);
        } else {
            assertTrue(MessageConsumers.MESSAGES.size() >= 2);
        }
    }

    @RepeatedTest(5)
    public void testRequestReply() throws InterruptedException {
        MessageConsumers.MESSAGES.clear();
        Uni<String> uni1 = eventBus.<String> request("req", "foo").map(Message::body);
        Uni<String> uni2 = eventBus.<String> request("req", "bar").map(Message::body);
        Uni<String> uni3 = eventBus.<String> request("req", "baz").map(Message::body);

        Uni.combine().all().unis(uni1, uni2, uni3).asTuple()
                .map(tuple -> {
                    assertEquals("FOO", tuple.getItem1());
                    assertEquals("BAR", tuple.getItem2());
                    assertEquals("BAZ", tuple.getItem3());
                    return "done";
                })
                .await().atMost(Duration.ofSeconds(3));

        if (Runtime.getRuntime().availableProcessors() > 1) {
            assertEquals(3, MessageConsumers.MESSAGES.size());
        } else {
            assertTrue(MessageConsumers.MESSAGES.size() >= 2);
        }
    }

    @ApplicationScoped
    static class MessageConsumers {

        static volatile CountDownLatch latch;

        static final Set<String> MESSAGES = new ConcurrentHashSet<>();

        @ConsumeEvent("pub")
        void pub1(String name) {
            MESSAGES.add(Thread.currentThread().getName());
            latch.countDown();
        }

        @ConsumeEvent("pub")
        void pub2(String name) {
            MESSAGES.add(Thread.currentThread().getName());
            latch.countDown();
        }

        @ConsumeEvent("pub")
        @Blocking
        void pub3(String name) {
            MESSAGES.add(Thread.currentThread().getName());
            latch.countDown();
        }

        @ConsumeEvent("send")
        void send1(String name) {
            MESSAGES.add(Thread.currentThread().getName());
            latch.countDown();
        }

        @ConsumeEvent("send")
        void send2(String name) {
            MESSAGES.add(Thread.currentThread().getName());
            latch.countDown();
        }

        @ConsumeEvent("send")
        @Blocking
        void send3(String name) {
            MESSAGES.add(Thread.currentThread().getName());
            latch.countDown();
        }

        @ConsumeEvent("req")
        String req1(String name) {
            MESSAGES.add(Thread.currentThread().getName());
            return name.toUpperCase();
        }

        @ConsumeEvent("req")
        String req2(String name) {
            MESSAGES.add(Thread.currentThread().getName());
            return name.toUpperCase();
        }

        @ConsumeEvent("req")
        @Blocking
        String req3(String name) {
            MESSAGES.add(Thread.currentThread().getName());
            return name.toUpperCase();
        }
    }

}
