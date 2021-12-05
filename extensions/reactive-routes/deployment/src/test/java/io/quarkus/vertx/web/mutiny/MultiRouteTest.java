package io.quarkus.vertx.web.mutiny;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class MultiRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SimpleBean.class));

    @Test
    public void testMultiRoute() {
        when().get("/hello").then().statusCode(200)
                .body(is("Hello world!"))
                .header("content-type", is(nullValue()));
        when().get("/hellos").then().statusCode(200)
                .body(is("helloworld!"))
                .header("content-type", is(nullValue()));
        when().get("/no-hello").then().statusCode(204).body(hasLength(0))
                .header("content-type", is(nullValue()));
        // status already sent, but not the end of the array
        when().get("/hello-and-fail").then().statusCode(200)
                .body(containsString("Hello"));

        when().get("/buffer").then().statusCode(200).body(is("Buffer"))
                .header("content-type", is(nullValue()));
        when().get("/buffers").then().statusCode(200).body(is("Buffer Buffer Buffer."));
        when().get("/buffers-and-fail").then().statusCode(200).body(containsString("Buffer"));

        when().get("/mutiny-buffer").then().statusCode(200).body(is("BufferBuffer"));

        when().get("/void").then().statusCode(204).body(hasLength(0));

        when().get("/people").then().statusCode(200)
                .body(containsString("{\"name\":\"superman\",\"id\":1}"))
                .body(containsString("{\"name\":\"batman\",\"id\":2}"))
                .body(containsString("{\"name\":\"spiderman\",\"id\":3}"))
                .header("content-type", is(nullValue()));

        when().get("/failure").then().statusCode(500).body(containsString("boom"));
        when().get("/null").then().statusCode(500).body(containsString(NullPointerException.class.getName()));
        when().get("/sync-failure").then().statusCode(500).body(containsString("boom"));

    }

    static class SimpleBean {

        @Route(path = "hello")
        Multi<String> hello(RoutingContext context) {
            return Multi.createFrom().item("Hello world!");
        }

        @Route(path = "hellos")
        Multi<String> hellos() {
            return Multi.createFrom().items("hello", "world", "!");
        }

        @Route(path = "no-hello")
        Multi<String> noHello() {
            return Multi.createFrom().empty();
        }

        @Route(path = "hello-and-fail")
        Multi<String> helloAndFail(RoutingContext context) {
            return Multi.createBy().concatenating().streams(
                    Multi.createFrom().item("Hello"),
                    Multi.createFrom().failure(new IOException("boom")));
        }

        @Route(path = "buffer")
        Multi<Buffer> buffer(RoutingContext context) {
            return Multi.createFrom().item(Buffer.buffer("Buffer"));
        }

        @Route(path = "buffers")
        Multi<Buffer> buffers(RoutingContext context) {
            return Multi.createFrom()
                    .items(Buffer.buffer("Buffer"), Buffer.buffer(" Buffer"), Buffer.buffer(" Buffer."));
        }

        @Route(path = "buffers-and-fail")
        Multi<Buffer> buffersAndFail(RoutingContext context) {
            return Multi.createBy().concatenating().collectFailures().streams(
                    Multi.createFrom().items(Buffer.buffer("Buffer"), Buffer.buffer(" Buffer"),
                            Buffer.buffer(" Buffer.")),
                    Multi.createFrom().failure(new IOException("boom")));

        }

        @Route(path = "mutiny-buffer")
        Multi<io.vertx.mutiny.core.buffer.Buffer> bufferMutiny(RoutingContext context) {
            return Multi.createFrom().items(io.vertx.mutiny.core.buffer.Buffer.buffer("Buffer"),
                    io.vertx.mutiny.core.buffer.Buffer.buffer("Buffer"));
        }

        @Route(path = "void")
        Multi<Void> multiVoid(RoutingContext context) {
            return Multi.createFrom().range(0, 200)
                    .onItem().ignore();
        }

        @Route(path = "/people")
        Multi<Person> people(RoutingContext context) {
            return Multi.createFrom().items(
                    new Person("superman", 1),
                    new Person("batman", 2),
                    new Person("spiderman", 3));
        }

        @Route(path = "/failure")
        Multi<Person> fail(RoutingContext context) {
            return Multi.createFrom().failure(new IOException("boom"));
        }

        @Route(path = "/sync-failure")
        Multi<Person> failSync(RoutingContext context) {
            throw new IllegalStateException("boom");
        }

        @Route(path = "/null")
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
