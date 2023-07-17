package io.quarkus.smallrye.reactivemessaging.wiring;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
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

public class DisabledConnectorAttachmentOutgoingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyDummyConnector.class, MySource.class))
            .overrideConfigKey("quarkus.reactive-messaging.auto-connector-attachment", "false");

    @Inject
    @Connector("dummy")
    MyDummyConnector connector;

    @Inject
    MySource source;

    @Test
    public void testAutoAttachmentOfOutgoingChannel() {
        assertThatThrownBy(() -> source.generate())
                .hasCauseInstanceOf(DefinitionException.class);
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
        Emitter<Integer> emitter;

        public void generate() {
            for (int i = 0; i < 5; i++) {
                emitter.send(i);
            }
        }
    }

}
