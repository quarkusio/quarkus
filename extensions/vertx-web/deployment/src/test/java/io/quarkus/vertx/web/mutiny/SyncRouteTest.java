package io.quarkus.vertx.web.mutiny;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class SyncRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SimpleBean.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testSynchronousRoute() {
        when().get("hello-sync").then().statusCode(200)
                .body(is("Sync Hello world"))
                .header("content-type", is(nullValue()));

        when().get("hello-buffer-sync").then().statusCode(200).body(is("Sync Buffer"))
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

    //https://github.com/quarkusio/quarkus/issues/10960
    @Test
    public void testNoAcceptHeaderContentType() throws Exception {
        //RESTAssured always sets an Accept header
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            CloseableHttpResponse res = client.execute(new HttpGet(this.url.toExternalForm() + "/default-content-type"));
            JsonParser parser = Json.createParser(res.getEntity().getContent());
            parser.next();
            JsonObject obj = parser.getObject();
            Assertions.assertEquals("neo", obj.getString("name"));
            Assertions.assertEquals(12345, obj.getInt("id"));
            Assertions.assertEquals("application/json", res.getFirstHeader("content-type").getValue());
        }
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

        @Route(path = "default-content-type", produces = "application/json")
        void hi(RoutingExchange routing) {
            routing.ok(new io.vertx.core.json.JsonObject()
                    .put("name", "neo")
                    .put("id", 12345).encode());
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
