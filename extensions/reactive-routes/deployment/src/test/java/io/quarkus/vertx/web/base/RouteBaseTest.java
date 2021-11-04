package io.quarkus.vertx.web.base;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.vertx.ext.web.RoutingContext;

public class RouteBaseTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SimpleBean.class));

    @Test
    public void testPath() {
        when().get("/hello").then().statusCode(200).body(is("Hello world!"));
        when().get("/simple/hello").then().statusCode(200).body(is("Hello another world!"));
        when().get("/some/foo").then().statusCode(200).body(is("Hello foo!"));
    }

    @Test
    public void testProduces() {
        given().header("Accept", "application/json").when().get("/ping").then().statusCode(200).body(is("{\"ping\":\"pong\"}"));
        given().header("Accept", "text/html").when().get("/ping").then().statusCode(200).body(is("<html>pong</html>"));
    }

    @RouteBase
    static class SimpleBean {

        @Route(path = "hello") // -> "/simple-bean/hello"
        void hello(RoutingContext context) {
            context.response().end("Hello world!");
        }

    }

    @RouteBase(path = "simple")
    static class AnotherBean {

        @Route // path is derived from the method name -> "/simple/hello"
        void hello(RoutingContext context) {
            context.response().end("Hello another world!");
        }

        @Route(regex = ".*foo") // base path is ignored
        void helloRegex(RoutingContext context) {
            context.response().end("Hello foo!");
        }

    }

    @RouteBase(produces = "text/html")
    static class HtmlEndpoint {

        @Route(path = "ping")
        void ping(RoutingContext context) {
            context.response().end("<html>pong</html>");
        }

    }

    @RouteBase(produces = "application/json")
    static class JsonEndpoint {

        @Route(path = "ping")
        void ping(RoutingContext context) {
            context.response().end("{\"ping\":\"pong\"}");
        }

    }

}
