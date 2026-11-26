package io.quarkus.smallrye.reactivemessaging.wiring;

import static org.awaitility.Awaitility.await;

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

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * A connector that gets its connector interface from a superclass, as {@code InMemoryConnector} does since SmallRye
 * Reactive Messaging 5.0.
 */
public class ConnectorInterfaceFromSuperclassTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BaseConnector.class, SubclassConnector.class, MySink.class))
            .overrideConfigKey("mp.messaging.incoming.my-sink.connector", "subclass");

    @Inject
    MySink sink;

    @Test
    public void testConnectorWithInheritedInterface() {
        await().until(() -> sink.items().size() == 5);
    }

    abstract static class BaseConnector implements IncomingConnectorFactory {

        @Override
        public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
            return ReactiveStreams
                    .fromPublisher(AdaptersToReactiveStreams.publisher(Multi.createFrom().range(0, 5).map(Message::of)));
        }
    }

    @ApplicationScoped
    @Connector("subclass")
    static class SubclassConnector extends BaseConnector {
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
