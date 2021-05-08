package io.quarkus.it.rest.client;

import java.net.URI;
import java.util.List;

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
                .end("Hello, " + (rc.getBodyAsString()).repeat(getCount(rc))));

        router.route("/call-client").blockingHandler(rc -> {
            String url = rc.getBody().toString();
            HelloClient client = RestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(HelloClient.class);
            String greeting = client.greeting("John", 2);
            rc.response().end(greeting);
        });
    }

    private int getCount(io.vertx.ext.web.RoutingContext rc) {
        List<String> countQueryParam = rc.queryParam("count");
        if (countQueryParam.isEmpty()) {
            return 1;
        }
        return Integer.parseInt(countQueryParam.get(0));
    }
}
