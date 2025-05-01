package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.HashMap;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.MatcherAssert;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Verifies that changes to the Content-Type header in a ContainerRequestFilter is propagated to the Multipart handling
 */
public class MultipartContentTypeHeaderUpdateTest {

    public static final String TO_BE_MULTIPART_MARKER = "application/to-be-multipart";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Resource.class, Input.class, MultipartDataInputTest.Result.class,
                            SetMultipartContentTypeFilter.class));

    @Test
    public void test() throws IOException {
        var uploadedContent = RestAssured
                .given()
                .contentType(TO_BE_MULTIPART_MARKER)
                .body("""
                        --content_boundary\r
                        Content-Disposition: form-data; name="text"; filename="my-file.txt"\r
                        \r
                        content\r
                        --content_boundary--\r
                        """.getBytes())
                .post("/test")
                .then()
                .extract()
                .asString();

        MatcherAssert.assertThat(uploadedContent, is("content"));
    }

    @Path("/test")
    public static class Resource {

        @POST
        public String testMultipart(Input input) {
            return input.text;
        }
    }

    public static class Input {
        @RestForm("text")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public String text;
    }

    @Provider
    @PreMatching
    public static class SetMultipartContentTypeFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext containerRequestContext) {
            var mediaType = containerRequestContext.getMediaType();
            if (TO_BE_MULTIPART_MARKER.equals(mediaType.getType() + "/" + mediaType.getSubtype())) {
                var parameters = new HashMap<>(mediaType.getParameters());
                parameters.put("boundary", "content_boundary");
                var multipartContentType = new MediaType("multipart", "form-data", parameters);
                containerRequestContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, multipartContentType.toString());
            }
        }
    }
}
