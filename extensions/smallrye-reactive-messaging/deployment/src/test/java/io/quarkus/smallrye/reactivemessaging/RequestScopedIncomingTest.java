package io.quarkus.smallrye.reactivemessaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Blocking;

public class RequestScopedIncomingTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.messaging.request-scoped.enabled", "true")
            .withApplicationRoot(jar -> jar.addClasses(
                    RequestData.class,
                    EventLoopConsumer.class,
                    BlockingConsumer.class,
                    UniConsumer.class,
                    Producer.class));

    @Inject
    EventLoopConsumer eventLoopConsumer;

    @Inject
    BlockingConsumer blockingConsumer;

    @Inject
    UniConsumer uniConsumer;

    @Test
    void eventLoopIncomingCanUseRequestScopedBeans() {
        await().until(() -> eventLoopConsumer.received().size() == 2);
        assertThat(eventLoopConsumer.received()).containsExactly("event-loop-1", "event-loop-2");
    }

    @Test
    void blockingIncomingCanUseRequestScopedBeans() {
        await().until(() -> blockingConsumer.received().size() == 2);
        assertThat(blockingConsumer.received()).containsExactly("blocking-1", "blocking-2");
    }

    @Test
    void uniIncomingCanUseRequestScopedBeans() {
        await().until(() -> uniConsumer.received().size() == 2);
        assertThat(uniConsumer.received()).containsExactly("uni-1", "uni-2");
    }

    @RequestScoped
    public static class RequestData {

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @ApplicationScoped
    public static class EventLoopConsumer {

        private final List<String> received = new CopyOnWriteArrayList<>();

        @Inject
        RequestData requestData;

        @Incoming("event-loop-in")
        void consume(String payload) {
            requestData.setValue(payload);
            received.add(requestData.getValue());
        }

        List<String> received() {
            return received;
        }
    }

    @ApplicationScoped
    public static class BlockingConsumer {

        private final List<String> received = new CopyOnWriteArrayList<>();

        @Inject
        RequestData requestData;

        @Incoming("blocking-in")
        @Blocking
        void consume(String payload) {
            requestData.setValue(payload);
            received.add(requestData.getValue());
        }

        List<String> received() {
            return received;
        }
    }

    @ApplicationScoped
    public static class UniConsumer {

        private final List<String> received = new CopyOnWriteArrayList<>();

        @Inject
        RequestData requestData;

        @Incoming("uni-in")
        Uni<Void> consume(String payload) {
            requestData.setValue(payload);
            received.add(requestData.getValue());
            return Uni.createFrom().voidItem();
        }

        List<String> received() {
            return received;
        }
    }

    @ApplicationScoped
    public static class Producer {

        @Outgoing("event-loop-in")
        public Flow.Publisher<String> eventLoopMessages() {
            return Multi.createFrom().items("event-loop-1", "event-loop-2");
        }

        @Outgoing("blocking-in")
        public Flow.Publisher<String> blockingMessages() {
            return Multi.createFrom().items("blocking-1", "blocking-2");
        }

        @Outgoing("uni-in")
        public Flow.Publisher<String> uniMessages() {
            return Multi.createFrom().items("uni-1", "uni-2");
        }
    }
}
