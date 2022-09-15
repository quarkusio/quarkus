package io.quarkus.smallrye.reactivemessaging.wiring;

import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.MutinyEmitter;

public class ConnectorAttachmentMutinyEmitterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyDummyConnector.class, MySource.class));

    @Inject
    @Connector("dummy")
    MyDummyConnector connector;

    @Inject
    MySource source;

    @Test
    public void testAutoAttachmentOfOutgoingChannel() {
        source.generate();
        await().until(() -> connector.getList().size() == 5);
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
    static class MySource {

        @Channel("my-source")
        MutinyEmitter<Integer> emitter;

        public void generate() {
            for (int i = 0; i < 5; i++) {
                emitter.send(i).subscribeAsCompletionStage();
            }
        }
    }

}
