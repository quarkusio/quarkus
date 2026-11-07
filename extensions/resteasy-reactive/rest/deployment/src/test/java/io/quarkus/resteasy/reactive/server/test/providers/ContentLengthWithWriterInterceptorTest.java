package io.quarkus.resteasy.reactive.server.test.providers;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ContentLengthWithWriterInterceptorTest {

    private static final String BODY = "a".repeat(20000);

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ContentLengthResource.class, WithWriterInterceptor.class, WriterInterceptor.class));

    @Test
    public void testContentLengthIsNotCombinedWithChunkedEncoding() {
        RestAssured.get("/content-length")
                .then()
                .statusCode(200)
                .header("Content-Length", String.valueOf(BODY.length()))
                .header("Transfer-Encoding", Matchers.nullValue())
                .body(Matchers.equalTo(BODY));
    }

    @Path("/content-length")
    @WithWriterInterceptor
    public static class ContentLengthResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get() {
            return Response.ok(BODY).header("Content-Length", BODY.length()).build();
        }
    }
}
