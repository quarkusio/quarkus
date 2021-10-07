package io.quarkus.resteasy.reactive.server.test.mediatype;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;

import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;

public class InvalidContentTypeTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class);
                }
            });

    @Test
    public void test() {
        given().config(config().encoderConfig(encoderConfig().encodeContentTypeAs("invalid", ContentType.TEXT))).body("dummy")
                .contentType("invalid").post("/hello")
                .then()
                .statusCode(415);
    }

    @Path("hello")
    public static class HelloResource {

        @Consumes("text/plain")
        @POST
        public String hello(String body) {
            return body;
        }
    }
}
