package io.quarkus.vertx.web.params;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class RouteMethodParametersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleBean.class));

    @Test
    public void testRoutes() {
        when().get("/hello").then().statusCode(200).body(is("Hello world!"));
        when().get("/hello-response").then().statusCode(200).body(is("Hello world!"));
        when().get("/hello-rx-response").then().statusCode(200).body(is("Hello world!"));
        when().get("/hello-response-nonvoid?name=foo").then().statusCode(200).body(is("Hello foo!"));
        when().get("/hello-all").then().statusCode(200).body(is("ok"));

    }

    static class SimpleBean {

        @Route(path = "/hello")
        void hello1(HttpServerRequest request, HttpServerResponse response) {
            String name = request.getParam("name");
            response.setStatusCode(200).end("Hello " + (name != null ? name : "world") + "!");
        }

        @Route(path = "/hello-response")
        void hello2(HttpServerResponse response) {
            response.setStatusCode(200).end("Hello world!");
        }

        @Route(path = "/hello-rx-response")
        void hello3(io.vertx.reactivex.core.http.HttpServerResponse response) {
            response.setStatusCode(200).end("Hello world!");
        }

        @Route(path = "/hello-response-nonvoid")
        String hello4(HttpServerRequest request) {
            return "Hello " + request.getParam("name") + "!";
        }

        @Route(path = "/hello-all")
        String hello5(io.vertx.reactivex.core.http.HttpServerResponse rxResponse, RoutingContext routingContext,
                RoutingExchange routingExchange, HttpServerRequest request, HttpServerResponse response,
                io.vertx.reactivex.core.http.HttpServerRequest rxRequest) {
            assertNotNull(rxRequest);
            assertNotNull(rxResponse);
            assertNotNull(routingContext);
            assertNotNull(routingExchange);
            assertNotNull(request);
            assertNotNull(response);
            return "ok";
        }

    }

}
