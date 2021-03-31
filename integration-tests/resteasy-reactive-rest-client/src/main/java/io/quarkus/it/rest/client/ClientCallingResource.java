package io.quarkus.it.rest.client;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

@ApplicationScoped
public class ClientCallingResource {
    private static final ObjectMapper mapper = new JsonMapper();

    private static final String[] RESPONSES = { "cortland", "cortland2" };
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
            Apple swappedApple = client.swapApple(new Apple("lobo"));
            client.completionApple(new Apple("lobo2")).whenComplete((apple2, t) -> {
                if (t == null) {
                    try {
                        rc.response().putHeader("content-type", "application/json")
                                .end(mapper.writeValueAsString(Arrays.asList(swappedApple, apple2)));
                        return;
                    } catch (JsonProcessingException e) {
                        t = e;
                    }
                }
                rc.response().putHeader("content-type", "text/plain").setStatusCode(500).end(t.getMessage());
            });
        });
    }

}
