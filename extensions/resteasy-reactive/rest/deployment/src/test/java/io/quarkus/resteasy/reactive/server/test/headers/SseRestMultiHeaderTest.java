package io.quarkus.resteasy.reactive.server.test.headers;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestMulti;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Multi;

/**
 * Headers and status set on a {@link RestMulti} must be sent for server-sent events too, not only for the other
 * streaming media types.
 */
public class SseRestMultiHeaderTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class));

    @Test
    public void sseHonoursRestMultiHeadersAndStatus() {
        RestAssured.given()
                .accept(MediaType.SERVER_SENT_EVENTS)
                .get("/sse-headers/sse")
                .then()
                .statusCode(222)
                .header("Foo", "Bar")
                .contentType(Matchers.startsWith(MediaType.SERVER_SENT_EVENTS));
    }

    @Test
    public void streamingHonoursRestMultiHeadersAndStatus() {
        RestAssured.given()
                .get("/sse-headers/plain")
                .then()
                .statusCode(222)
                .header("Foo", "Bar");
    }

    @Path("/sse-headers")
    public static class Resource {

        @GET
        @Path("/sse")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public RestMulti<String> sse() {
            return RestMulti.fromMultiData(Multi.createFrom().items("a", "b"))
                    .header("Foo", "Bar")
                    .status(222)
                    .build();
        }

        @GET
        @Path("/plain")
        public RestMulti<String> plain() {
            return RestMulti.fromMultiData(Multi.createFrom().items("a", "b"))
                    .header("Foo", "Bar")
                    .status(222)
                    .build();
        }
    }
}
