package org.jboss.resteasy.reactive.server.vertx.test.mediatype;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.http.ContentType;

public class InvalidAcceptTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(HelloResource.class);
        }
    });

    @Test
    public void test() {
        given().config(config().encoderConfig(encoderConfig().encodeContentTypeAs("invalid", ContentType.TEXT)))
                .body("dummy").accept("invalid").get("/hello").then().statusCode(406);
    }

    @Path("hello")
    public static class HelloResource {

        @Produces("text/plain")
        @GET
        public String hello() {
            return "hello";
        }
    }
}
