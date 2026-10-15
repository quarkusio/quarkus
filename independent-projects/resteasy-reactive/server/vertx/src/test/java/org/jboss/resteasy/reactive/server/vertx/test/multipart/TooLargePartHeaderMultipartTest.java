package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.smallrye.common.annotation.NonBlocking;

public class TooLargePartHeaderMultipartTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class);
                }
            });

    @Test
    public void testOversizedPartHeaderReturns413() {
        String boundary = "testboundary123";
        String hugeHeaderValue = "V".repeat(40000);
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "X-Pad: " + hugeHeaderValue + "\r\n"
                + "\r\n"
                + "Alice\r\n"
                + "--" + boundary + "--\r\n";

        given()
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(body.getBytes(StandardCharsets.UTF_8))
                .when()
                .post("/test")
                .then()
                .statusCode(413);
    }

    @Test
    public void testNormalPartHeaderSucceeds() {
        given()
                .multiPart("name", "Alice")
                .accept("text/plain")
                .when()
                .post("/test")
                .then()
                .statusCode(200);
    }

    @Path("/test")
    public static class Resource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        @NonBlocking
        public String hello(@RestForm String name) {
            return "hello " + name;
        }
    }
}
