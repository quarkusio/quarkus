package org.jboss.resteasy.reactive.server.vertx.test;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Duration;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;

public class Http3ConnectionHandlingTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResource.class));

    @Test
    public void testNonBlockingEndpoint() {
        get("/test/non-blocking")
                .then()
                .statusCode(200)
                .body(equalTo("non-blocking-ok"));
    }

    @Test
    public void testReactiveEndpoint() {
        get("/test/reactive")
                .then()
                .statusCode(200)
                .body(equalTo("reactive-ok"));
    }

    @Test
    public void testDelayedEndpoint() {
        get("/test/delayed")
                .then()
                .statusCode(200)
                .body(equalTo("delayed-ok"));
    }

    @Test
    public void testStreamingEndpoint() {
        get("/test/stream")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    public void testConnectionAccess() {
        get("/test/connection")
                .then()
                .statusCode(200)
                .body(equalTo("connection-ok"));
    }

    @Path("test")
    public static class TestResource {

        @GET
        @Path("non-blocking")
        @Produces(MediaType.TEXT_PLAIN)
        public Uni<String> nonBlocking() {
            return Uni.createFrom().item("non-blocking-ok");
        }

        @GET
        @Path("reactive")
        @Produces(MediaType.TEXT_PLAIN)
        public Uni<String> reactive() {
            return Uni.createFrom().item(() -> "reactive-ok");
        }

        @GET
        @Path("delayed")
        @Produces(MediaType.TEXT_PLAIN)
        public Uni<String> delayed() {
            return Uni.createFrom().item("delayed-ok")
                    .onItem().delayIt().by(Duration.ofMillis(10));
        }

        @GET
        @Path("stream")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @RestStreamElementType(MediaType.TEXT_PLAIN)
        public Multi<String> stream() {
            return Multi.createFrom().items("event1", "event2", "event3");
        }

        @GET
        @Path("connection")
        @Produces(MediaType.TEXT_PLAIN)
        public Uni<String> connection(@Context HttpServerRequest request) {
            Object conn = request.connection();
            if (conn == null) {
                return Uni.createFrom().item("connection-null");
            }
            return Uni.createFrom().item("connection-ok");
        }
    }
}
