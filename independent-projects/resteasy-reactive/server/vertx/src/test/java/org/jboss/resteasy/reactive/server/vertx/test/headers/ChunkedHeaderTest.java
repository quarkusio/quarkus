package org.jboss.resteasy.reactive.server.vertx.test.headers;

import static io.restassured.RestAssured.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
            return Response.ok("hello").header("Transfer-Encoding", "chunked").build();
        }
    }
}
