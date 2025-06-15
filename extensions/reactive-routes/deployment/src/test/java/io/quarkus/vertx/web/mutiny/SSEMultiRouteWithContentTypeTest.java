package io.quarkus.vertx.web.mutiny;

import static io.quarkus.vertx.web.ReactiveRoutes.EVENT_STREAM;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasLength;
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

public class SSEMultiRouteWithContentTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SimpleBean.class));

    @Test
    public void testSSEMultiRoute() {
        when().get("/hello").then().statusCode(200).body(is("data: Hello world!\nid: 0\n\n")).header("content-type",
                "text/event-stream");

        when().get("/hellos").then().statusCode(200).body(containsString(
        // @formatter:off
                        "data: hello\nid: 0\n\n"
                            + "data: world\nid: 1\n\n"
                            + "data: !\nid: 2\n\n"))
                        // @formatter:on
                .header("content-type", "text/event-stream");

        when().get("/no-hello").then().statusCode(200).body(hasLength(0)).header("content-type", "text/event-stream");

        // We get the item followed by the exception
        when().get("/hello-and-fail").then().statusCode(200).body(containsString("id: 0"))
                .body(not(containsString("boom")));

        when().get("/buffer").then().statusCode(200).body(is("data: Buffer\nid: 0\n\n")).header("content-type",
                is("text/event-stream"));

        when().get("/buffers").then().statusCode(200)
                .body(is("data: Buffer\nid: 0\n\ndata: Buffer\nid: 1\n\ndata: Buffer.\nid: 2\n\n"))
                .header("content-type", is("text/event-stream"));

        when().get("/mutiny-buffer").then().statusCode(200).body(is("data: Buffer\nid: 0\n\ndata: Mutiny\nid: 1\n\n"))
                .header("content-type", is("text/event-stream"));

        when().get("/void").then().statusCode(204).body(hasLength(0));

        when().get("/people").then().statusCode(200).body(is(
        // @formatter:off
                                "data: {\"name\":\"superman\",\"id\":1}\nid: 0\n\n" +
                                "data: {\"name\":\"batman\",\"id\":2}\nid: 1\n\n" +
                                "data: {\"name\":\"spiderman\",\"id\":3}\nid: 2\n\n"))
                        // @formatter:on
                .header("content-type", is("text/event-stream"));

        when().get("/people-content-type").then().statusCode(200).body(is(
        // @formatter:off
                        "data: {\"name\":\"superman\",\"id\":1}\nid: 0\n\n" +
                                "data: {\"name\":\"batman\",\"id\":2}\nid: 1\n\n" +
                                "data: {\"name\":\"spiderman\",\"id\":3}\nid: 2\n\n"))
                // @formatter:on
                .header("content-type", is("text/event-stream;charset=utf-8"));

        when().get("/people-as-event").then().statusCode(200).body(is(
        // @formatter:off
                        "event: person\ndata: {\"name\":\"superman\",\"id\":1}\nid: 1\n\n" +
                                "event: person\ndata: {\"name\":\"batman\",\"id\":2}\nid: 2\n\n" +
                                "event: person\ndata: {\"name\":\"spiderman\",\"id\":3}\nid: 3\n\n"))
                // @formatter:on
                .header("content-type", is("text/event-stream"));

        when().get("/people-as-event-without-id").then().statusCode(200).body(is(
        // @formatter:off
                        "event: person\ndata: {\"name\":\"superman\",\"id\":1}\nid: 0\n\n" +
                                "event: person\ndata: {\"name\":\"batman\",\"id\":2}\nid: 1\n\n" +
                                "event: person\ndata: {\"name\":\"spiderman\",\"id\":3}\nid: 2\n\n"))
                // @formatter:on
                .header("content-type", is("text/event-stream"));

        when().get("/people-as-event-without-event").then().statusCode(200).body(is(
        // @formatter:off
                        "data: {\"name\":\"superman\",\"id\":1}\nid: 1\n\n" +
                                "data: {\"name\":\"batman\",\"id\":2}\nid: 2\n\n" +
                                "data: {\"name\":\"spiderman\",\"id\":3}\nid: 3\n\n"))
                // @formatter:on
                .header("content-type", is("text/event-stream"));

        when().get("/failure").then().statusCode(500).body(containsString("boom"));
        when().get("/null").then().statusCode(500).body(containsString(NullPointerException.class.getName()));
        when().get("/sync-failure").then().statusCode(500).body(containsString("boom"));

    }

    static class SimpleBean {

        @Route(path = "hello", produces = EVENT_STREAM)
        Multi<String> hello() {
            return Multi.createFrom().item("Hello world!");
        }

        @Route(path = "hellos", produces = EVENT_STREAM)
        Multi<String> hellos() {
            return Multi.createFrom().items("hello", "world", "!");
        }

        @Route(path = "no-hello", produces = EVENT_STREAM)
        Multi<String> noHello() {
            return Multi.createFrom().empty();
        }

        @Route(path = "hello-and-fail", produces = EVENT_STREAM)
        Multi<String> helloAndFail() {
            return Multi.createBy().concatenating().streams(Multi.createFrom().item("Hello"),
                    Multi.createFrom().failure(() -> new IOException("boom")));
        }

        @Route(path = "buffer", produces = EVENT_STREAM)
        Multi<Buffer> buffer() {
            return Multi.createFrom().item(Buffer.buffer("Buffer"));
        }

        @Route(path = "buffers", produces = EVENT_STREAM)
        Multi<Buffer> buffers() {
            return Multi.createFrom().items(Buffer.buffer("Buffer"), Buffer.buffer("Buffer"), Buffer.buffer("Buffer."));
        }

        @Route(path = "mutiny-buffer", produces = EVENT_STREAM)
        Multi<io.vertx.mutiny.core.buffer.Buffer> bufferMutiny() {
            return Multi.createFrom().items(io.vertx.mutiny.core.buffer.Buffer.buffer("Buffer"),
                    io.vertx.mutiny.core.buffer.Buffer.buffer("Mutiny"));
        }

        @Route(path = "void", produces = EVENT_STREAM)
        Multi<Void> multiVoid() {
            return Multi.createFrom().range(0, 200).onItem().ignore();
        }

        @Route(path = "/people", produces = EVENT_STREAM)
        Multi<Person> people() {
            return Multi.createFrom().items(new Person("superman", 1), new Person("batman", 2),
                    new Person("spiderman", 3));
        }

        @Route(path = "/people-as-event", produces = EVENT_STREAM)
        Multi<PersonAsEvent> peopleAsEvent() {
            return Multi.createFrom().items(new PersonAsEvent("superman", 1), new PersonAsEvent("batman", 2),
                    new PersonAsEvent("spiderman", 3));
        }

        @Route(path = "/people-as-event-without-id", produces = EVENT_STREAM)
        Multi<PersonAsEventWithoutId> peopleAsEventWithoutId() {
            return Multi.createFrom().items(new PersonAsEventWithoutId("superman", 1),
                    new PersonAsEventWithoutId("batman", 2), new PersonAsEventWithoutId("spiderman", 3));
        }

        @Route(path = "/people-as-event-without-event", produces = EVENT_STREAM)
        Multi<PersonAsEventWithoutEvent> peopleAsEventWithoutEvent() {
            return Multi.createFrom().items(new PersonAsEventWithoutEvent("superman", 1),
                    new PersonAsEventWithoutEvent("batman", 2), new PersonAsEventWithoutEvent("spiderman", 3));
        }

        @Route(path = "/people-content-type", produces = EVENT_STREAM)
        Multi<Person> peopleWithContentType(RoutingContext context) {
            context.response().putHeader("content-type", "text/event-stream;charset=utf-8");
            return Multi.createFrom().items(new Person("superman", 1), new Person("batman", 2),
                    new Person("spiderman", 3));
        }

        @Route(path = "/failure", produces = EVENT_STREAM)
        Multi<Person> fail() {
            return Multi.createFrom().failure(new IOException("boom"));
        }

        @Route(path = "/sync-failure", produces = EVENT_STREAM)
        Multi<Person> failSync() {
            throw new IllegalStateException("boom");
        }

        @Route(path = "/null", produces = EVENT_STREAM)
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

    static class PersonAsEvent implements ReactiveRoutes.ServerSentEvent<Person> {
        public String name;
        public int id;

        public PersonAsEvent(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public Person data() {
            return new Person(name, id);
        }

        @Override
        public long id() {
            return id;
        }

        @Override
        public String event() {
            return "person";
        }
    }

    static class PersonAsEventWithoutId implements ReactiveRoutes.ServerSentEvent<Person> {
        public String name;
        public int id;

        public PersonAsEventWithoutId(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public Person data() {
            return new Person(name, id);
        }

        @Override
        public String event() {
            return "person";
        }
    }

    static class PersonAsEventWithoutEvent implements ReactiveRoutes.ServerSentEvent<Person> {
        public String name;
        public int id;

        public PersonAsEventWithoutEvent(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public Person data() {
            return new Person(name, id);
        }

        @Override
        public long id() {
            return id;
        }
    }

}
