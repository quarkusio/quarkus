package io.quarkus.resteasy.reactive.server.test.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * A {@code Range} request for a {@link File} must be answered with {@code 206 Partial Content} even when the
 * JAX-RS {@link Response} has been materialized before the entity is written, either because a response filter
 * accessed it or because the resource method returned a {@link Response} itself.
 * See <a href="https://github.com/quarkusio/quarkus/issues/45791">GitHub issue #45791</a>.
 */
public class FileRangeWithResponseTestCase {

    private static final String FILE = "src/test/resources/lorem.txt";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RangeResource.class, EntityReadingResponseFilter.class));

    @Test
    public void fileWithResponseFilterAccessingTheEntity() throws Exception {
        assertPartialContent("/range/file");
    }

    @Test
    public void fileWrappedInResponse() throws Exception {
        assertPartialContent("/range/response");
    }

    @Test
    public void pathWithResponseFilterAccessingTheEntity() throws Exception {
        assertPartialContent("/range/path");
    }

    @Test
    public void unsatisfiableRangeIsNotPartialContent() throws Exception {
        String content = Files.readString(Paths.get(FILE));
        given().header("Range", "bytes=" + (content.length() + 1) + "-")
                .get("/range/file")
                .then()
                .statusCode(200)
                .header("Content-Range", (String) null)
                .body(equalTo(content));
    }

    private static void assertPartialContent(String path) throws Exception {
        String content = Files.readString(Paths.get(FILE));
        given().header("Range", "bytes=0-9")
                .get(path)
                .then()
                .statusCode(206)
                .header("Content-Range", "bytes 0-9/" + content.length())
                .header("Content-Length", "10")
                .body(equalTo(content.substring(0, 10)));
        given().get(path)
                .then()
                .statusCode(200)
                .header("Content-Range", (String) null)
                .body(equalTo(content));
    }

    @Path("range")
    public static class RangeResource {

        @GET
        @Path("file")
        @Produces(MediaType.TEXT_PLAIN)
        public File file() {
            return new File(FILE);
        }

        @GET
        @Path("path")
        @Produces(MediaType.TEXT_PLAIN)
        public java.nio.file.Path path() {
            return Paths.get(FILE);
        }

        @GET
        @Path("response")
        @Produces(MediaType.TEXT_PLAIN)
        public Response response() {
            return Response.ok(new File(FILE)).build();
        }
    }

    public static class EntityReadingResponseFilter {

        @ServerResponseFilter
        public void filter(ContainerResponseContext responseContext) {
            // merely reading the entity materializes the JAX-RS response
            responseContext.getEntity();
        }
    }
}
