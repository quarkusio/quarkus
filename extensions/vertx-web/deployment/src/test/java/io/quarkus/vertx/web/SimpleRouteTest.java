package io.quarkus.vertx.web;

import static io.vertx.core.http.HttpMethod.DELETE;
import static org.hamcrest.Matchers.is;

import javax.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.runtime.Route;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class SimpleRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SimpleBean.class));

    @Test
    public void testSimpleRoute() {
        RestAssured.when().get("/hello").then().statusCode(200).body(is("Hello world!"));
        RestAssured.when().get("/foo?name=foo").then().statusCode(200).body(is("Hello foo!"));
        RestAssured.when().get("/bar").then().statusCode(200).body(is("Hello bar!"));
        RestAssured.when().get("/delete").then().statusCode(404);
        RestAssured.when().delete("/delete").then().statusCode(200).body(is("deleted"));
    }

    static class SimpleBean {

        @Route(path = "/hello")
        @Route(path = "/foo")
        void hello(RoutingContext ctx) {
            String name = ctx.request().getParam("name");
            ctx.response().setStatusCode(200).end("Hello " + (name != null ? name : "world") + "!");
        }

        @Route(path = "/delete", methods = DELETE)
        void deleteHttpMethod(RoutingContext ctx) {
            ctx.response().setStatusCode(200).end("deleted");
        }

        void addBar(@Observes Router router) {
            router.get("/bar").handler(ctx -> ctx.response().setStatusCode(200).end("Hello bar!"));
        }

    }

}
