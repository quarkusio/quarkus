package org.jboss.resteasy.reactive.server.vertx.test.mediatype;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.http.ContentType;

public class InvalidContentTypeTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class, SubResource.class);
                }
            });

    @Test
    public void test() {
        given().config(config().encoderConfig(encoderConfig().encodeContentTypeAs("invalid", ContentType.TEXT))).body("dummy")
                .contentType("invalid").post("/hello")
                .then()
                .statusCode(415);
    }

    @Test
    public void testSubResource() {
        given().config(config().encoderConfig(encoderConfig().encodeContentTypeAs("invalid", ContentType.TEXT))).body("dummy")
                .contentType("invalid").post("/hello/sub")
                .then()
                .statusCode(415);
    }

    @Path("hello")
    public static class HelloResource {

        @Consumes(MediaType.TEXT_PLAIN)
        @POST
        public String hello(String body) {
            return body;
        }

        @Consumes(MediaType.TEXT_PLAIN)
        @Path("sub")
        public SubResource helloSub() {
            return new SubResource();
        }
    }

    private static class SubResource {
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        public String hello(String body) {
            return "sub-answer";
        }
    }
}
