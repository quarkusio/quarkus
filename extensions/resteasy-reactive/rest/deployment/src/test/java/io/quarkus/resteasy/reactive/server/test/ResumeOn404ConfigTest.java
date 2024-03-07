package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.get;

import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.web.Router;

public class ResumeOn404ConfigTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, CustomRoute.class);
                }
            })
            .overrideRuntimeConfigKey("quarkus.rest.resume-on-404", "true");

    @Test
    public void matchingFromResteasyReactive() {
        get("/test")
                .then()
                .statusCode(200);
    }

    @Test
    public void matchingFromCustomRoute() {
        get("/main")
                .then()
                .statusCode(200);
    }

    @Test
    public void missing() {
        get("/dummy")
                .then()
                .statusCode(404);
    }

    @Path("/test")
    @RequestScoped
    public static class Resource {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "test";
        }

    }

    public static class CustomRoute {

        public void initMain(@Observes Router router) {
            router.get("/main").handler(rc -> rc.response().end("main"));
        }
    }
}
