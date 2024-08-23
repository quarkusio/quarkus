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

public class EndingSlashTest {

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
    public void test() {
        get("/hello/world")
                .then()
                .statusCode(200)
                .body(equalTo("Hello World!"));

        get("/hello/world/")
                .then()
                .statusCode(200)
                .body(equalTo("Hello World!"));

        get("/hello/world/1")
                .then()
                .statusCode(404);

        get("/hello/world/22")
                .then()
                .statusCode(404);
    }

    @Path("/hello")
    public static class TestResource {

        @GET
        @Path("/world/")
        @Produces(MediaType.TEXT_PLAIN)
        public String test() {
            return "Hello World!";
        }
    }
}
