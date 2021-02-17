package io.quarkus.it.rest.client;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

@ApplicationScoped
public class ClientCallingResource {
    private static final ObjectMapper mapper = new JsonMapper();

    @Inject
    Vertx vertx;

    void init(@Observes Router router) {
        router.post().handler(BodyHandler.create());

        router.post("/apples").handler(rc -> rc.response().putHeader("content-type", "application/json")
                .end("{\"cultivar\": \"cortland\"}"));

        router.route("/call-client").blockingHandler(rc -> {
            String url = rc.getBody().toString();
            SimpleClient client = RestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(SimpleClient.class);
            Apple swappedApple = client.swapApple(new Apple("lobo"));
            rc.response().end(jsonAsString(swappedApple));
        });
    }

    private String jsonAsString(Apple apple) {
        try {
            return mapper.writerFor(Apple.class).writeValueAsString(apple);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to stringify apple", e);
        }
    }
}
