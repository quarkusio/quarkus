package io.quarkus.vertx.web.mutiny;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class JsonMultiRouteWithContentTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SimpleBean.class));

    @Test
    public void testMultiRoute() {
        when().get("/hello").then().statusCode(200).body(is("[\"Hello world!\"]")).header("content-type",
                "application/json");
        when().get("/hellos").then().statusCode(200).body(is("[\"hello\",\"world\",\"!\"]")).header("content-type",
                "application/json");
        when().get("/no-hello").then().statusCode(200).body(is("[]")).header("content-type", "application/json");
        // status already sent, but not the end of the array
        when().get("/hello-and-fail").then().statusCode(200).body(containsString("[\"Hello\""))
                .body(not(containsString("]")));

        when().get("/buffers").then().statusCode(500);

        when().get("/void").then().statusCode(200).body(is("[]"));

        when().get("/people").then().statusCode(200).body("size()", is(3)).body("[0].name", is("superman"))
                .body("[1].name", is("batman")).body("[2].name", is("spiderman"))
                .header("content-type", "application/json");

        when().get("/people-content-type").then().statusCode(200).body("size()", is(3)).body("[0].name", is("superman"))
                .body("[1].name", is("batman")).body("[2].name", is("spiderman"))
                .header("content-type", "application/json;charset=utf-8");

        when().get("/failure").then().statusCode(500).body(containsString("boom"));
        when().get("/null").then().statusCode(500).body(containsString(NullPointerException.class.getName()));
        when().get("/sync-failure").then().statusCode(500).body(containsString("boom"));

    }

    static class SimpleBean {

        @Route(path = "hello", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<String> hello() {
            return Multi.createFrom().item("Hello world!");
        }

        @Route(path = "hellos", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<String> hellos() {
            return Multi.createFrom().items("hello", "world", "!");
        }

        @Route(path = "no-hello", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<String> noHello() {
            return Multi.createFrom().empty();
        }

        @Route(path = "hello-and-fail", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<String> helloAndFail() {
            return Multi.createBy().concatenating().streams(Multi.createFrom().item("Hello"),
                    Multi.createFrom().failure(new IOException("boom")));
        }

        @Route(path = "buffers", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<Buffer> buffers() {
            return Multi.createFrom().items(Buffer.buffer("Buffer"), Buffer.buffer(" Buffer"),
                    Buffer.buffer(" Buffer."));
        }

        @Route(path = "void", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<Void> multiVoid() {
            return Multi.createFrom().range(0, 200).onItem().ignore();
        }

        @Route(path = "/people", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<Person> people() {
            return Multi.createFrom().items(new Person("superman", 1), new Person("batman", 2),
                    new Person("spiderman", 3));
        }

        @Route(path = "/people-content-type", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<Person> peopleWithContentType(RoutingContext context) {
            context.response().putHeader("content-type", "application/json;charset=utf-8");
            return Multi.createFrom().items(new Person("superman", 1), new Person("batman", 2),
                    new Person("spiderman", 3));
        }

        @Route(path = "/failure", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<Person> fail() {
            return Multi.createFrom().failure(new IOException("boom"));
        }

        @Route(path = "/sync-failure", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<Person> failSync() {
            throw new IllegalStateException("boom");
        }

        @Route(path = "/null", produces = ReactiveRoutes.APPLICATION_JSON)
        Multi<String> uniNull() {
            return null;
        }

    }

    static class Person {
        public String name;
        public int id;

        public Person(String name, int id) {
            this.name = name;
            this.id = id;
        }
    }

}
