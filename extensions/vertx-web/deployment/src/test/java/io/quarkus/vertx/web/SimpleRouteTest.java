package io.quarkus.vertx.web;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static org.hamcrest.Matchers.is;

import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class SimpleRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SimpleBean.class,
                    SimpleEventBusBean.class, SimpleRoutesBean.class, Transformer.class));

    @Test
    public void testSimpleRoute() {
        when().get("/hello").then().statusCode(200).body(is("Hello world!"));
        when().get("/no-slash").then().statusCode(200).body(is("Hello world!"));
        when().get("/rx-hello").then().statusCode(200).body(is("Hello world!"));
        when().get("/bzuk").then().statusCode(200).body(is("Hello world!"));
        when().get("/hello-event-bus?name=ping").then().statusCode(200).body(is("Hello PING!"));
        when().get("/foo?name=foo").then().statusCode(200).body(is("Hello foo!"));
        when().get("/bar").then().statusCode(200).body(is("Hello bar!"));
        when().get("/delete").then().statusCode(405);
        when().delete("/delete").then().statusCode(200).body(is("deleted"));
        when().get("/routes").then().statusCode(200)
                .body(Matchers.containsString("/hello-event-bus"));
        given().contentType("text/plain").body("world")
                .post("/body").then().body(is("Hello world!"));
        when().get("/request").then().statusCode(200).body(is("HellO!"));
    }

    static class SimpleBean {

        @Inject
        Transformer transformer;

        @Route(path = "/hello")
        @Route(path = "/foo")
        @Route(path = "no-slash")
        void hello(RoutingContext context) {
            String name = context.request().getParam("name");
            context.response().setStatusCode(200).end("Hello " + (name != null ? name : "world") + "!");
        }

        @Route(path = "/rx-hello")
        void rxHello(io.vertx.reactivex.ext.web.RoutingContext context) {
            String name = context.request().getParam("name");
            context.response().setStatusCode(200).end("Hello " + (name != null ? name : "world") + "!");
        }

        @Route // path is derived from the method name
        void bzuk(RoutingExchange exchange) {
            exchange.ok("Hello " + exchange.getParam("name").orElse("world") + "!");
        }

        @Route(path = "/delete", methods = DELETE)
        void deleteHttpMethod(RoutingExchange exchange) {
            exchange.ok("deleted");
        }

        @Route(path = "/body", methods = HttpMethod.POST, consumes = "text/plain")
        void post(RoutingContext context) {
            context.response().setStatusCode(200).end("Hello " + context.getBodyAsString() + "!");
        }

        @Route
        void request(RoutingContext context) {
            context.response().setStatusCode(200).end(transformer.transform("Hello!"));
        }

    }

    static class SimpleRoutesBean {

        @Inject
        Router router;

        @Route(path = "/routes", methods = GET)
        void getRoutes(RoutingContext context) {
            context.response().setStatusCode(200).end(
                    router.getRoutes().stream().map(r -> r.getPath()).filter(Objects::nonNull)
                            .collect(Collectors.joining(",")));
        }

        void addBar(@Observes Router router) {
            router.get("/bar").handler(ctx -> ctx.response().setStatusCode(200).end("Hello bar!"));
        }

    }

    static class SimpleEventBusBean {

        @Inject
        EventBus eventBus;

        @Route(path = "/hello-event-bus", methods = GET)
        void helloEventBus(RoutingExchange exchange) {
            eventBus.request("hello", exchange.getParam("name").orElse("missing"), ar -> {
                if (ar.succeeded()) {
                    exchange.ok(ar.result().body().toString());
                } else {
                    exchange.serverError().end(ar.cause().getMessage());
                }
            });
        }
    }

    static class HelloGenerator {

        @ConsumeEvent("hello")
        String generate(String name) {
            return "Hello " + name.toUpperCase() + "!";
        }

    }

    @RequestScoped
    static class Transformer {

        String transform(String message) {
            return message.replace('o', 'O');
        }

    }

}
