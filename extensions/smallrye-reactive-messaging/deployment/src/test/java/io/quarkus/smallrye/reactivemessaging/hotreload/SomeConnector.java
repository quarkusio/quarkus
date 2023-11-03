package io.quarkus.smallrye.reactivemessaging.hotreload;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;

@ApplicationScoped
@Connector("quarkus-test-connector")
public class SomeConnector implements OutgoingConnectorFactory, IncomingConnectorFactory {

    @Override
    public PublisherBuilder<? extends Message<?>> getPublisherBuilder(Config config) {
        Integer increment = config.getOptionalValue("increment", Integer.class).orElse(1);

        return ReactiveStreams.of(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .map(i -> i + increment)
                .map(Message::of);
    }

    @Override
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config config) {
        return ReactiveStreams.<Message<String>> builder()
                .forEach(s -> items.add(s.getPayload()));
    }

    private JsonArray items = new JsonArray();

    @Inject
    Router router;

    @PostConstruct
    public void init() {
        router.get("/").handler(rc -> rc.response().end(items.encode()));
    }
}
