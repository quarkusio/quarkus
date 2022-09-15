package io.quarkus.smallrye.reactivemessaging.wiring;

import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.InjectionPoint;
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
import org.reactivestreams.Publisher;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.ChannelRegistry;
import io.smallrye.reactive.messaging.EmitterConfiguration;
import io.smallrye.reactive.messaging.EmitterFactory;
import io.smallrye.reactive.messaging.EmitterType;
import io.smallrye.reactive.messaging.MessagePublisherProvider;
import io.smallrye.reactive.messaging.annotations.EmitterFactoryFor;
import io.smallrye.reactive.messaging.providers.extension.ChannelProducer;

public class ConnectorAttachmentCustomEmitterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyDummyConnector.class, MySink.class,
                            CustomEmitter.class, CustomEmitterImpl.class, CustomEmitterFactory.class));

    @Inject
    @Connector("dummy")
    MyDummyConnector connector;

    @Test
    public void testAutoAttachmentOfOutgoingChannel() {
        await().until(() -> connector.getList().size() == 5);
    }

    @ApplicationScoped
    static class MySink {

        @Channel("sink")
        CustomEmitter<Integer> channel;

        @PostConstruct
        public void init() {
            assert Objects.nonNull(channel);
        }
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

    interface CustomEmitter<T> extends EmitterType {

    }

    static class CustomEmitterImpl<T> implements MessagePublisherProvider<T>, CustomEmitter<T> {

        @Override
        public Publisher<Message<? extends T>> getPublisher() {
            return Multi.createFrom().range(0, 5).map(Message::of).map(m -> (Message<T>) m);
        }
    }

    @ApplicationScoped
    @EmitterFactoryFor(CustomEmitter.class)
    static class CustomEmitterFactory implements EmitterFactory<CustomEmitterImpl<Object>> {
        @Inject
        ChannelRegistry channelRegistry;

        @Override
        public CustomEmitterImpl<Object> createEmitter(EmitterConfiguration configuration, long defaultBufferSize) {
            return new CustomEmitterImpl<>();
        }

        @Produces
        @Typed(CustomEmitter.class)
        @Channel("") // Stream name is ignored during type-safe resolution
        <T> CustomEmitter<T> produceEmitter(InjectionPoint injectionPoint) {
            String channelName = ChannelProducer.getChannelName(injectionPoint);
            return channelRegistry.getEmitter(channelName, CustomEmitter.class);
        }

    }
}
