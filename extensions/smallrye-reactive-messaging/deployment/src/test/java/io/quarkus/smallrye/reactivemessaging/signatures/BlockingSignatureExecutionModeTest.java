package io.quarkus.smallrye.reactivemessaging.signatures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.reactivemessaging.config.DumbConnector;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class BlockingSignatureExecutionModeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DumbConnector.class,
                            ProducerOnC.class,
                            BlockingConsumerFromConnector.class,
                            ConsumerFromConnector.class,
                            ConsumerFromInnerChannel.class))
            // .overrideConfigKey("mp.messaging.incoming.a.connector", "dummy") // discovered by the extension
            .overrideConfigKey("mp.messaging.incoming.a.values", "bonjour")
            .overrideConfigKey("mp.messaging.incoming.b.connector", "dummy")
            .overrideConfigKey("mp.messaging.incoming.b.values", "bonjour")
            .overrideConfigKey("mp.messaging.incoming.c.connector", "dummy")
            .overrideConfigKey("mp.messaging.incoming.c.values", "bonjour");

    @Inject
    BlockingConsumerFromConnector blockingConsumerFromConnector;

    @Test
    public void testBlockingSignatureFromConnector() {
        await().until(() -> blockingConsumerFromConnector.list().size() == 2);
        List<String> threadNames = blockingConsumerFromConnector.threads().stream().distinct().toList();
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("executor-thread-")).isTrue();
        }
    }

    @ApplicationScoped
    public static class BlockingConsumerFromConnector {
        private final List<String> list = new CopyOnWriteArrayList<>();
        private final List<String> threads = new CopyOnWriteArrayList<>();

        @Incoming("a")
        public void produce(String s) {
            threads.add(Thread.currentThread().getName());
            list.add(s);
        }

        public List<String> threads() {
            return threads;
        }

        public List<String> list() {
            return list;
        }
    }

    @Inject
    ConsumerFromConnector consumerFromConnector;

    @Test
    public void testNonBlockingSignatureFromConnector() {
        await().until(() -> consumerFromConnector.list().size() == 2);
        List<String> threadNames = consumerFromConnector.threads().stream().distinct().toList();
        assertThat(threadNames).containsOnly(Thread.currentThread().getName());
    }

    @ApplicationScoped
    public static class ConsumerFromConnector {
        private final List<String> list = new CopyOnWriteArrayList<>();
        private final List<String> threads = new CopyOnWriteArrayList<>();

        @Incoming("b")
        public Uni<Void> produce(String s) {
            threads.add(Thread.currentThread().getName());
            list.add(s);
            return Uni.createFrom().voidItem();
        }

        public List<String> threads() {
            return threads;
        }

        public List<String> list() {
            return list;
        }
    }

    @Inject
    NonBlockingConsumerFromConnector nonBlockingConsumerFromConnector;

    @Test
    public void testNonBlockingAnnotationFromConnector() {
        await().until(() -> nonBlockingConsumerFromConnector.list().size() == 2);
        List<String> threadNames = nonBlockingConsumerFromConnector.threads().stream().distinct().toList();
        assertThat(threadNames).containsOnly(Thread.currentThread().getName());
    }

    @ApplicationScoped
    public static class NonBlockingConsumerFromConnector {
        private final List<String> list = new CopyOnWriteArrayList<>();
        private final List<String> threads = new CopyOnWriteArrayList<>();

        @Incoming("c")
        @NonBlocking
        public void produce(String s) {
            threads.add(Thread.currentThread().getName());
            list.add(s);
        }

        public List<String> threads() {
            return threads;
        }

        public List<String> list() {
            return list;
        }
    }

    @Inject
    ConsumerFromInnerChannel consumerFromInnerChannel;

    @Test
    public void testBlockingSignatureFromInnerChannel() {
        await().until(() -> consumerFromInnerChannel.list().size() == 3);
        assertThat(consumerFromInnerChannel.list()).containsExactly("d", "e", "f");
        List<String> threadNames = consumerFromInnerChannel.threads().stream().distinct().toList();
        assertThat(threadNames).containsOnly(Thread.currentThread().getName());
    }

    @ApplicationScoped
    public static class ConsumerFromInnerChannel {

        private final List<String> list = new CopyOnWriteArrayList<>();
        private final List<String> threads = new CopyOnWriteArrayList<>();

        @Incoming("d")
        public void produce(String s) {
            threads.add(Thread.currentThread().getName());
            list.add(s);
        }

        public List<String> threads() {
            return threads;
        }

        public List<String> list() {
            return list;
        }
    }

    @ApplicationScoped
    private static class ProducerOnC {

        @Outgoing("d")
        public Flow.Publisher<String> produce() {
            return Multi.createFrom().items("d", "e", "f");
        }

    }
}
