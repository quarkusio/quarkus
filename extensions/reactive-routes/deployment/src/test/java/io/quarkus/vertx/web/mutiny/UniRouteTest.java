package io.quarkus.vertx.web.mutiny;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class UniRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SimpleBean.class));

    @Test
    public void testUniRoute() {
        when().get("/hello").then().statusCode(200).body(is("Hello world!"));
        when().get("/hello-buffer").then().statusCode(200).body(is("Buffer"));
        when().get("/hello-on-pool").then().statusCode(200).body(is("Pool"));
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

        when().get("/null").then().statusCode(500).body(containsString(NullPointerException.class.getName()));
        when().get("/uni-null").then().statusCode(500);
        when().get("/void").then().statusCode(204).body(hasLength(0));
    }

    static class SimpleBean {

        @Route(path = "hello")
        Uni<String> hello(RoutingContext context) {
            return Uni.createFrom().item("Hello world!");
        }

        @Route(path = "hello-buffer")
        Uni<Buffer> helloWithBuffer(RoutingContext context) {
            return Uni.createFrom().item(Buffer.buffer("Buffer"));
        }

        @Route(path = "hello-mutiny-buffer")
        Uni<io.vertx.mutiny.core.buffer.Buffer> helloWithMutinyBuffer(RoutingContext context) {
            return Uni.createFrom().item(io.vertx.mutiny.core.buffer.Buffer.buffer("Mutiny Buffer"));
        }

        @Route(path = "hello-on-pool")
        Uni<String> helloOnPool() {
            return Uni.createFrom().item("Pool")
                    .emitOn(Infrastructure.getDefaultExecutor());
        }

        @Route(path = "failure")
        Uni<String> fail(RoutingContext context) {
            return Uni.createFrom().<String> failure(new IOException("boom"))
                    .emitOn(Infrastructure.getDefaultExecutor());
        }

        @Route(path = "sync-failure")
        Uni<String> failUniSync(RoutingContext context) {
            throw new IllegalStateException("boom");
        }

        @Route(path = "null")
        Uni<String> uniNull(RoutingContext context) {
            return null;
        }

        @Route(path = "void")
        Uni<Void> uniOfVoid() {
            return Uni.createFrom().nullItem();
        }

        @Route(path = "uni-null")
        Uni<String> produceNull(RoutingContext context) {
            return Uni.createFrom().nullItem();
        }

        @Route(path = "person", produces = "application/json")
        Uni<Person> getPersonAsUni(RoutingContext context) {
            return Uni.createFrom().item(() -> new Person("neo", 12345)).emitOn(Infrastructure.getDefaultExecutor());
        }

        @Route(path = "person-content-type-set", produces = "application/json")
        Uni<Person> getPersonAsUniUtf8(RoutingContext context) {
            return Uni.createFrom().item(() -> new Person("neo", 12345))
                    .onItem()
                    .invoke(x -> context.response().putHeader("content-type", "application/json;charset=utf-8"))
                    .emitOn(Infrastructure.getDefaultExecutor());
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
