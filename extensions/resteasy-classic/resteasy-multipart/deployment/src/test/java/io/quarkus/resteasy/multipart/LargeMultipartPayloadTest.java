package io.quarkus.resteasy.multipart;

import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class LargeMultipartPayloadTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addAsResource(new StringAsset("""
                                    quarkus.http.limits.max-body-size=30M
                                    """),
                                    "application.properties");
                }
            });

    @Test
    public void testConnectionClosedOnException() {
        RestAssured
                .given()
                .multiPart("content", twentyMegaBytes())
                .post("/test/multipart")
                .then()
                .statusCode(500);
    }

    private static String twentyMegaBytes() {
        return new String(new byte[20_000_000]);
    }

    @Path("/test")
    public static class Resource {
        @POST
        @Path("/multipart")
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String postForm(@MultipartForm final FormBody ignored) {
            return "ignored";
        }
    }

    public static class FormBody {

        @FormParam("content")
        public String content;

    }

    @Priority(Priorities.USER)
    @Provider
    public static class Filter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext containerRequestContext) {
            throw new RuntimeException("Expected exception");
        }
    }

}
