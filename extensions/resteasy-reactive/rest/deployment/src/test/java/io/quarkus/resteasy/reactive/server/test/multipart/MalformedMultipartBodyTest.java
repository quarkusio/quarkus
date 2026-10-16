package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MalformedMultipartBodyTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(MalformedMultipartResource.class);
                }

            });

    @Test
    public void testMalformedMultipartHeaderNoColon() {
        String boundary = "----WebKitFormBoundary0x1234";
        byte[] body = ("--" + boundary + "\r\n" +
                "JustNoColonHere\r\n" +
                "--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);

        given()
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(body)
                .when()
                .post("/upload")
                .then()
                .statusCode(400);
    }

    @Test
    public void testMalformedMultipartBoundaryInContent() {
        String boundary = "----WebKitFormBoundary0x1234";
        byte[] body = ("--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"a.bin\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n" +
                "hello\r\n--" + boundary + "Xworld\r\n" +
                "--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);

        given()
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(body)
                .when()
                .post("/upload")
                .then()
                .statusCode(400);
    }

    @Path("/upload")
    public static class MalformedMultipartResource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String upload(@RestForm String dummy) {
            return "ok";
        }
    }
}
