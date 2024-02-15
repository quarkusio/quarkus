package io.quarkus.it.rest.client.main;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.arc.Arc;
import io.quarkus.it.rest.client.main.MyResponseExceptionMapper.MyException;
import io.quarkus.it.rest.client.main.selfsigned.ExternalSelfSignedClient;
import io.quarkus.it.rest.client.main.wronghost.WrongHostClient;
import io.quarkus.it.rest.client.main.wronghost.WrongHostRejectedClient;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
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
    ClientWithClientLogger clientWithClientLogger;

    @RestClient
    ClientWithExceptionMapper clientWithExceptionMapper;

    @RestClient
    FaultToleranceClient faultToleranceClient;

    @RestClient
    FaultToleranceOnInterfaceClient faultToleranceOnInterfaceClient;

    @RestClient
    ExternalSelfSignedClient externalSelfSignedClient;

    @RestClient
    WrongHostClient wrongHostClient;

    @RestClient
    WrongHostRejectedClient wrongHostRejectedClient;

    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    void init(@Observes Router router) {
        router.post().handler(BodyHandler.create());

        router.get("/unprocessable").handler(rc -> rc.response().setStatusCode(422).end("the entity was unprocessable"));

        router.get("/client-logger").handler(rc -> {
            rc.response().end("Hello World!");
        });

        router.get("/correlation").handler(rc -> {
            rc.response().end(rc.request().getHeader(CorrelationIdClient.CORRELATION_ID_HEADER_NAME));
        });

        router.post("/call-client-with-global-client-logger").blockingHandler(rc -> {
            String url = rc.body().asString();
            ClientWithClientLogger client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(ClientWithClientLogger.class);
            Arc.container().instance(MyClientLogger.class).get().reset();
            client.call();
            if (Arc.container().instance(MyClientLogger.class).get().wasUsed()) {
                success(rc, "global client logger was used");
            } else {
                fail(rc, "global client logger was not used");
            }
        });

        router.post("/call-client-with-explicit-client-logger").blockingHandler(rc -> {
            String url = rc.body().asString();
            MyClientLogger explicitClientLogger = new MyClientLogger();
            ClientWithClientLogger client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .clientLogger(explicitClientLogger)
                    .build(ClientWithClientLogger.class);
            client.call();
            if (explicitClientLogger.wasUsed()) {
                success(rc, "explicit client logger was used");
            } else {
                fail(rc, "explicit client logger was not used");
            }
        });

        router.post("/call-cdi-client-with-global-client-logger").blockingHandler(rc -> {
            Arc.container().instance(MyClientLogger.class).get().reset();
            clientWithClientLogger.call();
            if (Arc.container().instance(MyClientLogger.class).get().wasUsed()) {
                success(rc, "global client logger was used");
            } else {
                fail(rc, "global client logger was not used");
            }
        });

        router.post("/call-client-with-exception-mapper").blockingHandler(rc -> {
            String url = rc.body().asString();
            ClientWithExceptionMapper client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create(url))
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
            String url = rc.body().asString();
            AppleClient client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create(url))
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
            Uni<Apple> apple10 = Uni.createFrom().item(client.restResponseApple().getEntity());
            Uni<Apple> apple11 = client.uniRestResponseApple().onItem().transform(RestResponse::getEntity);
            Uni.combine().all().unis(apple1, apple2, apple3, apple4, apple5, apple6, apple7, apple8, apple9, apple10, apple11)
                    .combinedWith(Function.identity())
                    .subscribe()
                    .with(list -> {
                        try {
                            rc.response().putHeader("content-type", "application/json")
                                    .end(mapper.writeValueAsString(list));
                        } catch (JsonProcessingException e) {
                            fail(rc, e.getMessage());
                        }
                    }, t -> fail(rc, t.getMessage()));
        });

        router.route("/call-client-retry").blockingHandler(rc -> {
            String url = rc.body().asString();
            AppleClient client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create(url + "/does-not-exist"))
                    .build(AppleClient.class);
            AtomicInteger count = new AtomicInteger(0);
            client.uniSwapApple(new Apple("lobo")).onFailure().retry().until(t -> count.incrementAndGet() <= 3)
                    .subscribe()
                    .with(m -> success(rc, count.toString()), t -> success(rc, count.toString()));
        });

        router.post("/hello").handler(rc -> rc.response().putHeader("content-type", MediaType.TEXT_PLAIN)
                .end("Hello, " + (rc.body().asString()).repeat(getCount(rc))));

        router.post("/hello/fromMessage").handler(rc -> rc.response().putHeader("content-type", MediaType.TEXT_PLAIN)
                .end(rc.body().asJsonObject().getString("message")));

        router.route("/call-hello-client").blockingHandler(rc -> {
            String url = rc.body().asString();
            HelloClient client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(HelloClient.class);
            String greeting = client.greeting("John", 2);
            rc.response().end(greeting);
        });

        router.route("/call-hello-client-trace").blockingHandler(rc -> {
            String url = rc.body().asString();
            HelloClient client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(HelloClient.class);
            String greeting = client.greeting("Mary", 3);
            rc.response().end(greeting);
        });

        router.route("/call-helloFromMessage-client").blockingHandler(rc -> {
            String url = rc.body().asString();
            HelloClient client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(HelloClient.class);
            String greeting = client.fromMessage(new HelloClient.Message("Hello world"));
            rc.response().end(greeting);
        });

        router.post("/params/param").handler(rc -> rc.response().putHeader("content-type", MediaType.TEXT_PLAIN)
                .end(getParam(rc)));

        router.route("/call-params-client-with-param-first").blockingHandler(rc -> {
            String url = rc.body().asString();
            ParamClient client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .build(ParamClient.class);
            String result = client.getParam(Param.FIRST);
            rc.response().end(result);
        });

        router.route("/rest-response").blockingHandler(rc -> {
            String url = rc.body().asString();
            RestResponseClient client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create(url))
                    .property("microprofile.rest.client.disable.default.mapper", true)
                    .build(RestResponseClient.class);
            RestResponse<String> restResponse = client.response();
            rc.response().end("" + restResponse.getStatus());
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

        router.route("/call-with-fault-tolerance").blockingHandler(rc -> {
            rc.end(faultToleranceClient.helloWithFallback());
        });

        router.route("/call-with-fault-tolerance-on-interface").blockingHandler(rc -> {
            String result = "";
            try {
                result = faultToleranceOnInterfaceClient.hello();
            } catch (Exception e) {
                result = e.getClass().getSimpleName();
            }
            rc.end(result);
        });

        router.get("/with%20space").handler(rc -> rc.response().setStatusCode(200).end());

        router.get("/self-signed").blockingHandler(
                rc -> rc.response().setStatusCode(200).end(String.valueOf(externalSelfSignedClient.invoke().getStatus())));

        router.get("/wrong-host").blockingHandler(
                rc -> rc.response().setStatusCode(200).end(String.valueOf(wrongHostClient.invoke().getStatus())));

        router.get("/wrong-host-rejected").blockingHandler(rc -> {
            try {
                int result = wrongHostRejectedClient.invoke().getStatus();
                rc.response().setStatusCode(200).end(String.valueOf(result));
            } catch (Exception e) {
                rc.response().setStatusCode(500).end(e.getCause().getClass().getSimpleName());
            }
        });
    }

    private Future<Void> success(RoutingContext rc, String body) {
        return rc.response().putHeader("content-type", "text-plain").end(body);
    }

    private int getCount(io.vertx.ext.web.RoutingContext rc) {
        List<String> countQueryParam = rc.queryParam("count");
        if (countQueryParam.isEmpty()) {
            return 1;
        }
        return Integer.parseInt(countQueryParam.get(0));
    }

    private String getParam(io.vertx.ext.web.RoutingContext rc) {
        return rc.queryParam("param").get(0);
    }

    private void callGet(RoutingContext rc, ClientWithExceptionMapper client) {
        try {
            String response = client.get();
            if ("MockAnswer".equals(response)) {
                rc.response().setStatusCode(503).end(response);
                return;
            }
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
