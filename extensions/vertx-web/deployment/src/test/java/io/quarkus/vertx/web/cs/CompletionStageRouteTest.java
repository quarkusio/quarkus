package io.quarkus.vertx.web.cs;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class CompletionStageRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SimpleBean.class));

    @Test
    public void testCsRoute() {
        when().get("/hello").then().statusCode(200).body(is("Hello world!"));
        when().get("/hello-buffer").then().statusCode(200).body(is("Buffer"));
        when().get("/hello-mutiny-buffer").then().statusCode(200).body(is("Mutiny Buffer"));

        when().get("/person").then().statusCode(200)
                .body("name", is("neo"))
                .body("id", is(12345))
                .header("content-type", "application/json");

        when().get("/person-content-type-set").then().statusCode(200)
                .body("name", is("neo"))
                .body("id", is(12345))
                .header("content-type", "application/json;charset=utf-8");

        when().get("/failure").then().statusCode(500).body(containsString("boom"));
        when().get("/sync-failure").then().statusCode(500).body(containsString("boom"));

        when().get("/null").then().statusCode(500).body(containsString("null"));
        when().get("/cs-null").then().statusCode(500);
        when().get("/void").then().statusCode(204).body(hasLength(0));
    }

    static class SimpleBean {

        @Route(path = "hello")
        CompletionStage<String> hello(RoutingContext context) {
            return CompletableFuture.completedFuture("Hello world!");
        }

        @Route(path = "hello-buffer")
        CompletionStage<Buffer> helloWithBuffer(RoutingContext context) {
            return CompletableFuture.completedFuture(Buffer.buffer("Buffer"));
        }

        @Route(path = "hello-mutiny-buffer")
        CompletionStage<io.vertx.mutiny.core.buffer.Buffer> helloWithMutinyBuffer(RoutingContext context) {
            return CompletableFuture.completedFuture(io.vertx.mutiny.core.buffer.Buffer.buffer("Mutiny Buffer"));
        }

        @Route(path = "failure")
        CompletionStage<String> fail(RoutingContext context) {
            CompletableFuture<String> ret = new CompletableFuture<>();
            ret.completeExceptionally(new IOException("boom"));
            return ret;
        }

        @Route(path = "sync-failure")
        CompletionStage<String> failCsSync(RoutingContext context) {
            throw new IllegalStateException("boom");
        }

        @Route(path = "null")
        CompletionStage<String> csNull(RoutingContext context) {
            return null;
        }

        @Route(path = "void")
        CompletionStage<Void> csOfVoid() {
            return CompletableFuture.completedFuture(null);
        }

        @Route(path = "cs-null")
        CompletionStage<String> produceNull(RoutingContext context) {
            return CompletableFuture.completedFuture(null);
        }

        @Route(path = "person", produces = "application/json")
        CompletionStage<Person> getPersonAsCs(RoutingContext context) {
            return CompletableFuture.completedFuture(new Person("neo", 12345));
        }

        @Route(path = "person-content-type-set", produces = "application/json")
        CompletionStage<Person> getPersonAsCsUtf8(RoutingContext context) {
            context.response().putHeader("content-type", "application/json;charset=utf-8");
            return CompletableFuture.completedFuture(new Person("neo", 12345));
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
