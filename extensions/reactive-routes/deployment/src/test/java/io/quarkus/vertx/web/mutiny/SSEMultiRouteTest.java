package io.quarkus.vertx.web.mutiny;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class SSEMultiRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SimpleBean.class));

    @Test
    public void testSSEMultiRoute() {
        when().get("/hello").then().statusCode(200)
                .body(is("data: Hello world!\nid: 0\n\n"))
                .header("content-type", "text/event-stream");

        when().get("/hellos").then().statusCode(200)
                .body(containsString(
                // @formatter:off
                        "data: hello\nid: 0\n\n"
                            + "data: world\nid: 1\n\n"
                            + "data: !\nid: 2\n\n"))
                        // @formatter:on
                .header("content-type", "text/event-stream");

        when().get("/no-hello").then().statusCode(200).body(hasLength(0))
                .header("content-type", "text/event-stream");

        // We get the item followed by the exception
        when().get("/hello-and-fail").then().statusCode(200)
                .body(containsString("id: 0"))
                .body(not(containsString("boom")));

        when().get("/buffer").then().statusCode(200)
                .body(is("data: Buffer\nid: 0\n\n"))
                .header("content-type", is("text/event-stream"));

        when().get("/buffers").then().statusCode(200)
                .body(is("data: Buffer\nid: 0\n\ndata: Buffer\nid: 1\n\ndata: Buffer.\nid: 2\n\n"))
                .header("content-type", is("text/event-stream"));

        when().get("/mutiny-buffer").then().statusCode(200)
                .body(is("data: Buffer\nid: 0\n\ndata: Mutiny\nid: 1\n\n"))
                .header("content-type", is("text/event-stream"));

        when().get("/void").then().statusCode(204).body(hasLength(0));

        when().get("/people").then().statusCode(200)
                .body(is(
                // @formatter:off
                                "data: {\"name\":\"superman\",\"id\":1}\nid: 0\n\n" +
                                "data: {\"name\":\"batman\",\"id\":2}\nid: 1\n\n" +
                                "data: {\"name\":\"spiderman\",\"id\":3}\nid: 2\n\n"))
                        // @formatter:on
                .header("content-type", is("text/event-stream"));

        when().get("/people-content-type").then().statusCode(200)
                .body(is(
                // @formatter:off
                        "data: {\"name\":\"superman\",\"id\":1}\nid: 0\n\n" +
                                "data: {\"name\":\"batman\",\"id\":2}\nid: 1\n\n" +
                                "data: {\"name\":\"spiderman\",\"id\":3}\nid: 2\n\n"))
                // @formatter:on
                .header("content-type", is("text/event-stream;charset=utf-8"));

        when().get("/people-as-event").then().statusCode(200)
                .body(is(
                // @formatter:off
                        "event: person\ndata: {\"name\":\"superman\",\"id\":1}\nid: 1\n\n" +
                                "event: person\ndata: {\"name\":\"batman\",\"id\":2}\nid: 2\n\n" +
                                "event: person\ndata: {\"name\":\"spiderman\",\"id\":3}\nid: 3\n\n"))
                // @formatter:on
                .header("content-type", is("text/event-stream"));

        when().get("/people-as-event-without-id").then().statusCode(200)
                .body(is(
                // @formatter:off
                        "event: person\ndata: {\"name\":\"superman\",\"id\":1}\nid: 0\n\n" +
                                "event: person\ndata: {\"name\":\"batman\",\"id\":2}\nid: 1\n\n" +
                                "event: person\ndata: {\"name\":\"spiderman\",\"id\":3}\nid: 2\n\n"))
                // @formatter:on
                .header("content-type", is("text/event-stream"));

        when().get("/people-as-event-without-event").then().statusCode(200)
                .body(is(
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

        @Route(path = "hello")
        Multi<String> hello(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom().item("Hello world!"));
        }

        @Route(path = "hellos")
        Multi<String> hellos(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom().items("hello", "world", "!"));
        }

        @Route(path = "no-hello")
        Multi<String> noHello(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom().empty());
        }

        @Route(path = "hello-and-fail")
        Multi<String> helloAndFail(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createBy().concatenating().streams(
                    Multi.createFrom().item("Hello"),
                    Multi.createFrom().failure(() -> new IOException("boom"))));
        }

        @Route(path = "buffer")
        Multi<Buffer> buffer(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom().item(Buffer.buffer("Buffer")));
        }

        @Route(path = "buffers")
        Multi<Buffer> buffers(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom()
                    .items(Buffer.buffer("Buffer"), Buffer.buffer("Buffer"), Buffer.buffer("Buffer.")));
        }

        @Route(path = "mutiny-buffer")
        Multi<io.vertx.mutiny.core.buffer.Buffer> bufferMutiny(RoutingContext context) {
            return ReactiveRoutes
                    .asEventStream(Multi.createFrom().items(io.vertx.mutiny.core.buffer.Buffer.buffer("Buffer"),
                            io.vertx.mutiny.core.buffer.Buffer.buffer("Mutiny")));
        }

        @Route(path = "void")
        Multi<Void> multiVoid(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom().range(0, 200)
                    .onItem().ignore());
        }

        @Route(path = "/people")
        Multi<Person> people(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom().items(
                    new Person("superman", 1),
                    new Person("batman", 2),
                    new Person("spiderman", 3)));
        }

        @Route(path = "/people-as-event")
        Multi<PersonAsEvent> peopleAsEvent(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom().items(
                    new PersonAsEvent("superman", 1),
                    new PersonAsEvent("batman", 2),
                    new PersonAsEvent("spiderman", 3)));
        }

        @Route(path = "/people-as-event-without-id")
        Multi<PersonAsEventWithoutId> peopleAsEventWithoutId(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom().items(
                    new PersonAsEventWithoutId("superman", 1),
                    new PersonAsEventWithoutId("batman", 2),
                    new PersonAsEventWithoutId("spiderman", 3)));
        }

        @Route(path = "/people-as-event-without-event")
        Multi<PersonAsEventWithoutEvent> peopleAsEventWithoutEvent(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom().items(
                    new PersonAsEventWithoutEvent("superman", 1),
                    new PersonAsEventWithoutEvent("batman", 2),
                    new PersonAsEventWithoutEvent("spiderman", 3)));
        }

        @Route(path = "/people-content-type")
        Multi<Person> peopleWithContentType(RoutingContext context) {
            context.response().putHeader("content-type", "text/event-stream;charset=utf-8");
            return ReactiveRoutes.asEventStream(Multi.createFrom().items(
                    new Person("superman", 1),
                    new Person("batman", 2),
                    new Person("spiderman", 3)));
        }

        @Route(path = "/failure")
        Multi<Person> fail(RoutingContext context) {
            return ReactiveRoutes.asEventStream(Multi.createFrom().failure(new IOException("boom")));
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
