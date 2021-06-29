package io.quarkus.vertx.web.mutiny;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class NdjsonMultiRouteTest {

    public static final String CONTENT_TYPE_NDJSON = "application/x-ndjson";
    public static final String CONTENT_TYPE_STREAM_JSON = "application/stream+json";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SimpleBean.class));

    @Test
    public void testNdjsonMultiRoute() {
        when().get("/hello").then().statusCode(200)
                .body(is("\"Hello world!\"\n"))
                .header(HttpHeaders.CONTENT_TYPE.toString(), CONTENT_TYPE_NDJSON);

        when().get("/hellos").then().statusCode(200)
                .body(containsString(
                // @formatter:off
                        "\"hello\"\n"
                            + "\"world\"\n"
                            + "\"!\"\n"))
                        // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE.toString(), CONTENT_TYPE_NDJSON);

        when().get("/no-hello").then().statusCode(200).body(hasLength(0))
                .header(HttpHeaders.CONTENT_TYPE.toString(), CONTENT_TYPE_NDJSON);

        // We get the item followed by the exception
        when().get("/hello-and-fail").then().statusCode(200)
                .body(containsString("\"Hello\""))
                .body(containsString("boom"));

        when().get("/void").then().statusCode(204).body(hasLength(0));

        when().get("/people").then().statusCode(200)
                .body(is(
                // @formatter:off
                                "{\"name\":\"superman\",\"id\":1}\n" +
                                "{\"name\":\"batman\",\"id\":2}\n" +
                                "{\"name\":\"spiderman\",\"id\":3}\n"
                ))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE.toString(), CONTENT_TYPE_NDJSON);

        when().get("/people-content-type").then().statusCode(200)
                .body(is(
                // @formatter:off
                                "{\"name\":\"superman\",\"id\":1}\n" +
                                "{\"name\":\"batman\",\"id\":2}\n" +
                                "{\"name\":\"spiderman\",\"id\":3}\n"))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE.toString(), is(CONTENT_TYPE_NDJSON + ";charset=utf-8"));

        when().get("/people-content-type-stream-json").then().statusCode(200)
                .body(is(
                // @formatter:off
                        "{\"name\":\"superman\",\"id\":1}\n" +
                                "{\"name\":\"batman\",\"id\":2}\n" +
                                "{\"name\":\"spiderman\",\"id\":3}\n"))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE.toString(), CONTENT_TYPE_STREAM_JSON);

        when().get("/failure").then().statusCode(500).body(containsString("boom"));
        when().get("/null").then().statusCode(500).body(containsString("null"));
        when().get("/sync-failure").then().statusCode(500).body(containsString("null"));
    }

    static class SimpleBean {

        @Route(path = "hello")
        Multi<String> hello(RoutingContext context) {
            return ReactiveRoutes.asJsonStream(Multi.createFrom().item("Hello world!"));
        }

        @Route(path = "hellos")
        Multi<String> hellos(RoutingContext context) {
            return ReactiveRoutes.asJsonStream(Multi.createFrom().items("hello", "world", "!"));
        }

        @Route(path = "no-hello")
        Multi<String> noHello(RoutingContext context) {
            return ReactiveRoutes.asJsonStream(Multi.createFrom().empty());
        }

        @Route(path = "hello-and-fail")
        Multi<String> helloAndFail(RoutingContext context) {
            return ReactiveRoutes.asJsonStream(Multi.createBy().concatenating().streams(
                    Multi.createFrom().item("Hello"),
                    Multi.createFrom().failure(() -> new IOException("boom"))));
        }

        @Route(path = "void")
        Multi<Void> multiVoid(RoutingContext context) {
            return ReactiveRoutes.asJsonStream(Multi.createFrom().range(0, 200)
                    .onItem().ignore());
        }

        @Route(path = "/people")
        Multi<Person> people(RoutingContext context) {
            return ReactiveRoutes.asJsonStream(Multi.createFrom().items(
                    new Person("superman", 1),
                    new Person("batman", 2),
                    new Person("spiderman", 3)));
        }

        @Route(path = "/people-content-type")
        Multi<Person> peopleWithContentType(RoutingContext context) {
            context.response().putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_NDJSON + ";charset=utf-8");
            return ReactiveRoutes.asJsonStream(Multi.createFrom().items(
                    new Person("superman", 1),
                    new Person("batman", 2),
                    new Person("spiderman", 3)));
        }

        @Route(path = "/people-content-type-stream-json", produces = { CONTENT_TYPE_STREAM_JSON })
        Multi<Person> peopleWithContentTypeStreamJson(RoutingContext context) {
            return ReactiveRoutes.asJsonStream(Multi.createFrom().items(
                    new Person("superman", 1),
                    new Person("batman", 2),
                    new Person("spiderman", 3)));
        }

        @Route(path = "/failure")
        Multi<Person> fail(RoutingContext context) {
            return ReactiveRoutes.asJsonStream(Multi.createFrom().failure(new IOException("boom")));
        }

        @Route(path = "/sync-failure")
        Multi<Person> failSync(RoutingContext context) {
            throw new IllegalStateException("boom");
        }

        @Route(path = "/null")
        Multi<String> uniNull(RoutingContext context) {
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
