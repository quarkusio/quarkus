package io.quarkus.smallrye.reactivemessaging.hotreload;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.connector.InboundConnector;
import io.smallrye.reactive.messaging.connector.OutboundConnector;
import io.smallrye.reactive.messaging.providers.helpers.MultiUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;

@ApplicationScoped
@Connector("quarkus-test-connector")
public class SomeConnector implements InboundConnector, OutboundConnector {

    private final static List<String> items = new CopyOnWriteArrayList<>();

    @Inject
    Router router;

    @PostConstruct
    public void init() {
        router.get("/reset").handler(rc -> {
            items.clear();
            rc.response().end();
        });
        router.get("/").handler(rc -> rc.response().end(new JsonArray(List.copyOf(items)).encode()));
    }

    @Override
    public Flow.Publisher<? extends Message<?>> getPublisher(Config config) {
        Integer increment = config.getOptionalValue("increment", Integer.class).orElse(1);

        return Multi.createFrom().range(1, 101)
                .map(i -> i + increment)
                .map(Message::of);
    }

    @Override
    public Flow.Subscriber<? extends Message<?>> getSubscriber(Config config) {
        return MultiUtils.via(m -> m.invoke(msg -> items.add(msg.getPayload().toString())));
    }
}
