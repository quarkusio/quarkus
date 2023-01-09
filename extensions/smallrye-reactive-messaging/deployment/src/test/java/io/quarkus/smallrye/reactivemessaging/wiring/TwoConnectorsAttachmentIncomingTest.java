package io.quarkus.smallrye.reactivemessaging.wiring;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class TwoConnectorsAttachmentIncomingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MySecondDummyConnector.class, MySecondDummyConnector.class, MySink.class));

    @Inject
    MySink sink;

    @Test
    public void testAutoAttachmentOfIncomingChannel() {
        await()
                .pollDelay(Duration.ofMillis(100))
                .until(() -> sink.items().isEmpty());
    }

    @ApplicationScoped
    @Connector("dummy")
    static class MyDummyConnector implements IncomingConnectorFactory {

        @Override
        public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
            return ReactiveStreams.fromPublisher(Multi.createFrom().range(0, 5).map(Message::of));
        }
    }

    @ApplicationScoped
    @Connector("dummy-2")
    static class MySecondDummyConnector implements IncomingConnectorFactory {

        @Override
        public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
            return ReactiveStreams.fromPublisher(Multi.createFrom().range(0, 5).map(Message::of));
        }
    }

    @ApplicationScoped
    static class MySink {
        private final List<Integer> items = new CopyOnWriteArrayList<>();

        @Incoming("my-sink")
        public void sink(int l) {
            items.add(l);
        }

        public List<Integer> items() {
            return items;
        }
    }

}
