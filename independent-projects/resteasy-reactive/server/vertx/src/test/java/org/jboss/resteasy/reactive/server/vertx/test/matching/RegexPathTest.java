package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RegexPathTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(EndingSlashTest.TestResource.class);
                }
            });

    @Test
    public void test() {

        get("/hello/world/1")
                .then()
                .statusCode(200)
                .body(equalTo("Hello World! 1"));

        get("/hello/world/1/")
                .then()
                .statusCode(200)
                .body(equalTo("Hello World! 1"));

        get("/hello/")
                .then()
                .statusCode(404);

        get("/hello/1")
                .then()
                .statusCode(200)
                .body(equalTo("Hello! 1"));

        get("/hello/1/")
                .then()
                .statusCode(200)
                .body(equalTo("Hello! 1"));

        get("/hello/again/")
                .then()
                .statusCode(404);

        get("/hello/again/1")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Again! 1"));

        get("/hello/again/1/")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Again! 1"));

        get("/hello/again/2/surprise")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Again Surprise! 2"));

        get("/hello/again/2/surprise/")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Again Surprise! 2"));
    }

    @Path("/hello")
    public static class TestResource {

        @GET
        @Path("/world/{sample}")
        @Produces(MediaType.TEXT_PLAIN)
        public String helloWorld(int sample) {
            return "Hello World! " + sample;
        }

        @GET
        @Path("/{sample:\\d+}")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(int sample) {
            return "Hello! " + sample;
        }

        @GET
        @Path("/again/{sample:\\d+}")
        @Produces(MediaType.TEXT_PLAIN)
        public String helloAgain(int sample) {
            return "Hello Again! " + sample;
        }

        @GET
        @Path("/again/{sample:\\d+}/surprise")
        @Produces(MediaType.TEXT_PLAIN)
        public String helloAgainSurprise(int sample) {
            return "Hello Again Surprise! " + sample;
        }
    }
}
