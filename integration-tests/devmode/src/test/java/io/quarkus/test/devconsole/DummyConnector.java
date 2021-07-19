package io.quarkus.test.devconsole;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

@ApplicationScoped
@Connector("dummy")
public class DummyConnector implements IncomingConnectorFactory {

    @Override
    public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
        String values = config.getValue("values", String.class);
        return ReactiveStreams.of(values, values.toUpperCase())
                .map(Message::of);
    }
}
