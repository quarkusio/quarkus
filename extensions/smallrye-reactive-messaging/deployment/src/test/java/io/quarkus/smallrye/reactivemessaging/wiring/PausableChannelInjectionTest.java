package io.quarkus.smallrye.reactivemessaging.wiring;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.PausableChannel;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

public class PausableChannelInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyProcessor.class, MyDummyConnector.class)
                    .addAsResource(new StringAsset(
                            "mp.messaging.incoming.source.pausable=true\n" +
                                    "mp.messaging.incoming.source.connector=dummy\n"),
                            "application.properties"));

    @Inject
    MyProcessor processor;

    @Test
    public void testPausableChannelInjection() {
        // Pause the channel
        Assertions.assertThat(processor.pausable()).isNotNull();
    }

    @ApplicationScoped
    public static class MyProcessor {
        private final List<Integer> items = new CopyOnWriteArrayList<>();

        @Inject
        @Channel("source")
        PausableChannel pausable;

        @Incoming("source")
        public void consume(Integer item) {
            items.add(item);
        }

        public PausableChannel pausable() {
            return pausable;
        }

        public List<Integer> items() {
            return items;
        }
    }

    @ApplicationScoped
    @Connector("dummy")
    static class MyDummyConnector implements IncomingConnectorFactory {

        @Override
        public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
            return ReactiveStreams
                    .fromPublisher(AdaptersToReactiveStreams.publisher(Multi.createFrom().range(0, 5).map(Message::of)));
        }
    }

}
