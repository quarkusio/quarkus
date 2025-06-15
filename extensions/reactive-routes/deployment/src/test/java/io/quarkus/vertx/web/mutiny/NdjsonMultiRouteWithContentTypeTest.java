package io.quarkus.vertx.web.mutiny;

import static io.quarkus.vertx.web.ReactiveRoutes.JSON_STREAM;
import static io.quarkus.vertx.web.ReactiveRoutes.ND_JSON;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class NdjsonMultiRouteWithContentTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SimpleBean.class));

    @Test
    public void testNdjsonMultiRoute() {
        when().get("/hello").then().statusCode(200).body(is("\"Hello world!\"\n"))
                .header(HttpHeaders.CONTENT_TYPE.toString(), ND_JSON);

        when().get("/hellos").then().statusCode(200).body(containsString(
        // @formatter:off
                        "\"hello\"\n"
                            + "\"world\"\n"
                            + "\"!\"\n"))
                        // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE.toString(), ND_JSON);

        when().get("/no-hello").then().statusCode(200).body(hasLength(0)).header(HttpHeaders.CONTENT_TYPE.toString(),
                ND_JSON);

        // We get the item followed by the exception
        when().get("/hello-and-fail").then().statusCode(200).body(containsString("\"Hello\""))
                .body(not(containsString("boom")));

        when().get("/void").then().statusCode(204).body(hasLength(0));

        when().get("/people").then().statusCode(200).body(is(
        // @formatter:off
                                "{\"name\":\"superman\",\"id\":1}\n" +
                                "{\"name\":\"batman\",\"id\":2}\n" +
                                "{\"name\":\"spiderman\",\"id\":3}\n"
                ))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE.toString(), ND_JSON);

        when().get("/people-content-type").then().statusCode(200).body(is(
        // @formatter:off
                                "{\"name\":\"superman\",\"id\":1}\n" +
                                "{\"name\":\"batman\",\"id\":2}\n" +
                                "{\"name\":\"spiderman\",\"id\":3}\n"))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE.toString(), is(ND_JSON + ";charset=utf-8"));

        when().get("/people-content-type-stream-json").then().statusCode(200).body(is(
        // @formatter:off
                        "{\"name\":\"superman\",\"id\":1}\n" +
                                "{\"name\":\"batman\",\"id\":2}\n" +
                                "{\"name\":\"spiderman\",\"id\":3}\n"))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE.toString(), JSON_STREAM);

        when().get("/failure").then().statusCode(500).body(containsString("boom"));
        when().get("/null").then().statusCode(500).body(containsString(NullPointerException.class.getName()));
        when().get("/sync-failure").then().statusCode(500).body(containsString("boom"));
    }

    static class SimpleBean {

        @Route(path = "hello", produces = ND_JSON)
        Multi<String> hello() {
            return Multi.createFrom().item("Hello world!");
        }

        @Route(path = "hellos", produces = ND_JSON)
        Multi<String> hellos() {
            return Multi.createFrom().items("hello", "world", "!");
        }

        @Route(path = "no-hello", produces = ND_JSON)
        Multi<String> noHello() {
            return Multi.createFrom().empty();
        }

        @Route(path = "hello-and-fail", produces = ND_JSON)
        Multi<String> helloAndFail() {
            return Multi.createBy().concatenating().streams(Multi.createFrom().item("Hello"),
                    Multi.createFrom().failure(() -> new IOException("boom")));
        }

        @Route(path = "void", produces = ND_JSON)
        Multi<Void> multiVoid() {
            return Multi.createFrom().range(0, 200).onItem().ignore();
        }

        @Route(path = "/people", produces = ND_JSON)
        Multi<Person> people() {
            return Multi.createFrom().items(new Person("superman", 1), new Person("batman", 2),
                    new Person("spiderman", 3));
        }

        @Route(path = "/people-content-type", produces = ND_JSON)
        Multi<Person> peopleWithContentType(RoutingContext context) {
            context.response().putHeader(HttpHeaders.CONTENT_TYPE, ND_JSON + ";charset=utf-8");
            return Multi.createFrom().items(new Person("superman", 1), new Person("batman", 2),
                    new Person("spiderman", 3));
        }

        @Route(path = "/people-content-type-stream-json", produces = { JSON_STREAM })
        Multi<Person> peopleWithContentTypeStreamJson() {
            return Multi.createFrom().items(new Person("superman", 1), new Person("batman", 2),
                    new Person("spiderman", 3));
        }

        @Route(path = "/failure", produces = ND_JSON)
        Multi<Person> fail() {
            return Multi.createFrom().failure(new IOException("boom"));
        }

        @Route(path = "/sync-failure", produces = ND_JSON)
        Multi<Person> failSync() {
            throw new IllegalStateException("boom");
        }

        @Route(path = "/null", produces = ND_JSON)
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
