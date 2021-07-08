package io.quarkus.it.rest.client.main;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.it.rest.client.main.MyResponseExceptionMapper.MyException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@ApplicationScoped
public class ClientCallingResource {
    private static final ObjectMapper mapper = new JsonMapper();

    private static final String[] RESPONSES = { "cortland", "lobo", "golden delicious" };
    private final AtomicInteger count = new AtomicInteger(0);

    @RestClient
    ClientWithExceptionMapper clientWithExceptionMapper;

    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    void init(@Observes Router router) {
        router.post().handler(BodyHandler.create());

        router.get("/unprocessable").handler(rc -> rc.response().setStatusCode(422).end("the entity was unprocessable"));

        router.post("/call-client-with-exception-mapper").blockingHandler(rc -> {
            String url = rc.getBody().toString();
            ClientWithExceptionMapper client = RestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .register(MyResponseExceptionMapper.class)
                    .build(ClientWithExceptionMapper.class);
            callGet(rc, client);
        });

        router.post("/call-cdi-client-with-exception-mapper").blockingHandler(rc -> callGet(rc, clientWithExceptionMapper));

        router.post("/apples").handler(rc -> {
            int count = this.count.getAndIncrement();
            rc.response().putHeader("content-type", "application/json")
                    .end(String.format("{\"cultivar\": \"%s\"}", RESPONSES[count % RESPONSES.length]));
        });

        router.route("/call-client").blockingHandler(rc -> {
            String url = rc.getBody().toString();
            AppleClient client = RestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(AppleClient.class);
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
                    }, t -> fail(rc, t.getMessage()));
        });

        router.post("/hello").handler(rc -> rc.response().putHeader("content-type", MediaType.TEXT_PLAIN)
                .end("Hello, " + (rc.getBodyAsString()).repeat(getCount(rc))));

        router.route("/call-hello-client").blockingHandler(rc -> {
            String url = rc.getBody().toString();
            HelloClient client = RestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(HelloClient.class);
            String greeting = client.greeting("John", 2);
            rc.response().end(greeting);
        });

        router.route("/export-clear").blockingHandler(rc -> {
            inMemorySpanExporter.reset();
            rc.response().end();
        });

        router.route("/export").blockingHandler(rc -> {
            rc.response().putHeader("content-type", "application/json")
                    .end(Json.encodePrettily(inMemorySpanExporter.getFinishedSpanItems()
                            .stream().filter(sd -> !sd.getName().contains("export"))
                            .collect(Collectors.toList())));
        });
    }

    private int getCount(io.vertx.ext.web.RoutingContext rc) {
        List<String> countQueryParam = rc.queryParam("count");
        if (countQueryParam.isEmpty()) {
            return 1;
        }
        return Integer.parseInt(countQueryParam.get(0));
    }

    private void callGet(RoutingContext rc, ClientWithExceptionMapper client) {
        try {
            client.get();
        } catch (MyException expected) {
            rc.response().setStatusCode(200).end();
            return;
        } catch (Exception unexpected) {
            rc.response().setStatusCode(500).end("Expected MyException to be thrown, got " + unexpected.getClass());
            return;
        }
        rc.response().setStatusCode(500).end("Expected MyException to be thrown but no exception has been thrown");
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
