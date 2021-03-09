package io.quarkus.it.rest.client;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

@ApplicationScoped
public class ClientCallingResource {

    void init(@Observes Router router) {
        router.post().handler(BodyHandler.create());

        router.post("/hello").handler(rc -> rc.response().putHeader("content-type", MediaType.TEXT_PLAIN)
                .end("Hello, " + rc.getBodyAsString()));

        router.route("/call-client").blockingHandler(rc -> {
            String url = rc.getBody().toString();
            HelloClient client = RestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(HelloClient.class);
            String greeting = client.greeting("John");
            rc.response().end(greeting);
        });
    }
}
