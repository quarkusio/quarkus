package io.quarkus.vertx.web.mutiny;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class SyncRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SimpleBean.class));

    @Test
    public void testSynchronousRoute() {
        when().get("hello-sync").then().statusCode(200)
                .body(is("Sync Hello world"))
                .header("content-type", is(nullValue()));

        when().get("hello-buffer-sync").then().statusCode(200).body(is("Sync Buffer"))
                .header("content-type", is(nullValue()));

        when().get("hello-buffer-rx-sync").then().statusCode(200).body(is("Sync RX Buffer"))
                .header("content-type", is(nullValue()));

        when().get("hello-buffer-mutiny-sync").then().statusCode(200).body(is("Sync Mutiny Buffer"))
                .header("content-type", is(nullValue()));

        when().get("/person-sync").then().statusCode(200)
                .body("name", is("neo"))
                .body("id", is(12345))
                .header("content-type", "application/json");

        when().get("/person-sync-content-type-set").then().statusCode(200)
                .body("name", is("neo"))
                .body("id", is(12345))
                .header("content-type", "application/json;charset=utf-8");

        when().get("/fail-sync")
                .then().statusCode(500)
                .body(containsString("boom"));
    }

    static class SimpleBean {

        @Route(path = "hello-sync")
        String helloSync() {
            return "Sync Hello world";
        }

        @Route(path = "hello-buffer-sync")
        Buffer helloBufferSync(RoutingContext context) {
            return Buffer.buffer("Sync Buffer");
        }

        @Route(path = "hello-buffer-rx-sync")
        io.vertx.reactivex.core.buffer.Buffer helloRxBufferSync() {
            return io.vertx.reactivex.core.buffer.Buffer.buffer("Sync RX Buffer");
        }

        @Route(path = "hello-buffer-mutiny-sync")
        io.vertx.mutiny.core.buffer.Buffer helloMutinyBufferSync(RoutingContext context) {
            return io.vertx.mutiny.core.buffer.Buffer.buffer("Sync Mutiny Buffer");
        }

        @Route(path = "fail-sync")
        String failSync(RoutingContext context) {
            throw new IllegalStateException("boom");
        }

        @Route(path = "person-sync", produces = "application/json")
        Person getPerson() {
            return new Person("neo", 12345);
        }

        @Route(path = "person-sync-content-type-set", produces = "application/json")
        Person getPersonUtf8(RoutingContext context) {
            context.response().putHeader("content-type", "application/json;charset=utf-8");
            return new Person("neo", 12345);
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
