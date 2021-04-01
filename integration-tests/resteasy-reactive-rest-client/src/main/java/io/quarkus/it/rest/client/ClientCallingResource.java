package io.quarkus.it.rest.client;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@ApplicationScoped
public class ClientCallingResource {
    private static final ObjectMapper mapper = new JsonMapper();

    private static final String[] RESPONSES = { "cortland", "cortland2", "cortland3" };
    private final AtomicInteger count = new AtomicInteger(0);

    void init(@Observes Router router) {
        router.post().handler(BodyHandler.create());

        router.post("/apples").handler(rc -> {
            int count = this.count.getAndIncrement();
            rc.response().putHeader("content-type", "application/json")
                    .end(String.format("{\"cultivar\": \"%s\"}", RESPONSES[count % RESPONSES.length]));
        });

        router.route("/call-client").blockingHandler(rc -> {
            String url = rc.getBody().toString();
            SimpleClient client = RestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(SimpleClient.class);
            Uni<Apple> apple1 = Uni.createFrom().item(client.swapApple(new Apple("lobo")));
            Uni<Apple> apple2 = Uni.createFrom().completionStage(client.completionSwapApple(new Apple("lobo2")));
            Uni<Apple> apple3 = client.uniSwapApple(new Apple("lobo3"));
            Uni<Apple> apple4 = Uni.createFrom().item(client.someApple());
            Uni<Apple> apple5 = Uni.createFrom().completionStage(client.completionSomeApple());
            Uni<Apple> apple6 = client.uniSomeApple();
            Uni<Apple> apple7 = Uni.createFrom().item(client.stringApple()).onItem().transform(this::toApple);
            Uni<Apple> apple8 = Uni.createFrom().completionStage(client.completionStringApple()).onItem()
                    .transform(this::toApple);
            Uni<Apple> apple9 = client.uniStringApple().onItem().transform(this::toApple);
            Uni.combine().all().unis(apple1, apple2, apple3, apple4, apple5, apple6, apple7, apple8, apple9).asTuple()
                    .subscribe()
                    .with(tuple -> {
                        try {
                            rc.response().putHeader("content-type", "application/json")
                                    .end(mapper.writeValueAsString(tuple.asList()));
                        } catch (JsonProcessingException e) {
                            fail(rc, e.getMessage());
                        }
                    }, t -> {
                        fail(rc, t.getMessage());
                    });
        });
    }

    private void fail(RoutingContext rc, String message) {
        rc.response().putHeader("content-type", "text/plain").setStatusCode(500).end(message);
    }

    private Apple toApple(String s) {
        try {
            return mapper.readValue(s, Apple.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
