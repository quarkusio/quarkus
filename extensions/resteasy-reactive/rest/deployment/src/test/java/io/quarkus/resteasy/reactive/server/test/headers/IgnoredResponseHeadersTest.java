package io.quarkus.resteasy.reactive.server.test.headers;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.Headers;

public class IgnoredResponseHeadersTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class));

    @Test
    public void testResponse() {
        doTest("response");
    }

    @Test
    public void testRestResponse() {
        doTest("rest-response");
    }

    private static void doTest(String path) {
        Headers responseHeaders = when()
                .get("/resource/" + path)
                .then()
                .statusCode(200)
                .extract().headers();

        assertThat(responseHeaders.getList("Transfer-Encoding"))
                .extracting("value")
                .singleElement().isEqualTo("chunked");
    }

    @Path("resource")
    public static class Resource {

        @Path("response")
        @Produces(MediaType.TEXT_PLAIN)
        @GET
        public Response response() {
            return Response.ok(largeString())
                    .header("Transfer-Encoding", "chunked")
                    .header("Content-Type", "text/plain")
                    .build();
        }

        @Path("rest-response")
        @Produces(MediaType.TEXT_PLAIN)
        @GET
        public RestResponse<InputStream> restResponse() {
            return RestResponse.ResponseBuilder.ok(largeString())
                    .header("Transfer-Encoding", "chunked")
                    .header("Content-Type", "text/plain")
                    .build();
        }

        private static InputStream largeString() {
            String content = IntStream.range(1, 100_000).mapToObj(i -> "Hello no." + i).collect(Collectors.joining(","));
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
