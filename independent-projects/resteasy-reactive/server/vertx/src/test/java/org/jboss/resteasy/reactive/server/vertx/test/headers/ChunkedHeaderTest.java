package org.jboss.resteasy.reactive.server.vertx.test.headers;

import static io.restassured.RestAssured.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ChunkedHeaderTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest TEST = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResource.class));

    @Test
    public void testReturnUni() {
        given()
                .get("/test/hello")
                .then()
                .statusCode(200)
                .headers("Transfer-Encoding", "chunked")
                .headers("Content-Length", is(nullValue()));
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Path("hello")
        public Response hello() {
            return Response.ok(largeString()).header("Transfer-Encoding", "chunked").build();
        }

        private static InputStream largeString() {
            String content = IntStream.range(1, 100_000).mapToObj(i -> "Hello no." + i).collect(Collectors.joining(","));
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
