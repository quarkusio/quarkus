package org.jboss.resteasy.reactive.server.vertx.test.mediatype;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CharsetTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestResource.class);
                }
            });

    @Test
    public void testText() {
        String contentType = when().get("/test/text")
                .then()
                .statusCode(200)
                .extract().header("Content-Type");
        assertEquals("text/plain;charset=UTF-8", contentType);
    }

    @Test
    public void testResponseText() {
        String contentType = when().get("/test/response/text")
                .then()
                .statusCode(200)
                .extract().header("Content-Type");
        assertEquals("text/plain;charset=UTF-8", contentType);
    }

    @Test
    public void testJson() {
        String contentType = when().get("/test/json")
                .then()
                .statusCode(200)
                .extract().header("Content-Type");
        assertEquals("application/json", contentType);
    }

    @Test
    public void testResponseJson() {
        String contentType = when().get("/test/response/json")
                .then()
                .statusCode(200)
                .extract().header("Content-Type");
        assertEquals("application/json", contentType);
    }

    @Test
    public void testJsonWithExplicitCharset() {
        String contentType = when().get("/test/json/charset")
                .then()
                .statusCode(200)
                .extract().header("Content-Type");
        assertEquals("application/json;charset=UTF-8", contentType);
    }

    @Test
    public void testJsonSuffix() {
        String contentType = when().get("/test/problem")
                .then()
                .statusCode(200)
                .extract().header("Content-Type");
        assertEquals("application/problem+json", contentType);
    }

    @Test
    public void testXml() {
        String contentType = when().get("/test/xml")
                .then()
                .statusCode(200)
                .extract().header("Content-Type");
        assertEquals("application/xml;charset=UTF-8", contentType);
    }

    @Test
    public void testImage() {
        String contentType = when().get("/test/image")
                .then()
                .statusCode(200)
                .extract().header("Content-Type");
        assertEquals("image/png", contentType);
    }

    @Path("test")
    public static class TestResource {

        @Path("text")
        @Produces("text/plain")
        @GET
        public String textPlain() {
            return "text";
        }

        @Path("response/text")
        @Produces("text/plain")
        @GET
        public Response responseTextPlain() {
            return Response.ok("text").build();
        }

        @Path("json")
        @Produces("application/json")
        @GET
        public String json() {
            return "{\"foo\": \"bar\"}";
        }

        @Path("response/json")
        @Produces("application/json")
        @GET
        public Response responseJson() {
            return Response.ok("{\"foo\": \"bar\"}").build();
        }

        @Path("json/charset")
        @Produces("application/json;charset=UTF-8")
        @GET
        public String jsonWithExplicitCharset() {
            return "{\"foo\": \"bar\"}";
        }

        @Path("problem")
        @Produces("application/problem+json")
        @GET
        public String problemJson() {
            return "{\"title\": \"bar\"}";
        }

        @Path("xml")
        @Produces("application/xml")
        @GET
        public String xml() {
            return "<foo/>";
        }

        @Path("image")
        @Produces("image/png")
        @GET
        public Response imagePng() {
            return Response.ok("fake image".getBytes(StandardCharsets.UTF_8)).build();
        }

        @Path("response/image")
        @Produces("image/png")
        @GET
        public byte[] responseImagePng() {
            return "fake image".getBytes(StandardCharsets.UTF_8);
        }
    }
}
