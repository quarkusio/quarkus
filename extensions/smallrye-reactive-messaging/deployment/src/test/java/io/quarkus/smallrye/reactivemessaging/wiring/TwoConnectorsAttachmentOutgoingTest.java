package io.quarkus.smallrye.reactivemessaging.wiring;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class TwoConnectorsAttachmentOutgoingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyDummyConnector.class, MySecondDummyConnector.class, MySource.class));

    @Inject
    @Connector("dummy")
    MyDummyConnector connector;

    @Inject
    @Connector("dummy-2")
    MySecondDummyConnector connector2;

    @Test
    public void testAutoAttachmentOfOutgoingChannel() {
        await()
                .pollDelay(Duration.ofMillis(100))
                .until(() -> connector.getList().isEmpty());
        await()
                .pollDelay(Duration.ofMillis(100))
                .until(() -> connector2.getList().isEmpty());
    }

    @ApplicationScoped
    @Connector("dummy")
    static class MyDummyConnector implements OutgoingConnectorFactory {

        private final List<Message<?>> list = new CopyOnWriteArrayList<>();

        @Override
        public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config config) {
            return ReactiveStreams.<Message<?>> builder().forEach(list::add);
        }

        public List<Message<?>> getList() {
            return list;
        }
    }

    @ApplicationScoped
    @Connector("dummy-2")
    static class MySecondDummyConnector implements OutgoingConnectorFactory {

        private final List<Message<?>> list = new CopyOnWriteArrayList<>();

        @Override
        public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config config) {
            return ReactiveStreams.<Message<?>> builder().forEach(list::add);
        }

        public List<Message<?>> getList() {
            return list;
        }
    }

    @ApplicationScoped
    static class MySource {
        @Outgoing("my-source")
        public Multi<Integer> generate() {
            return Multi.createFrom().range(0, 5);
        }
    }

}
