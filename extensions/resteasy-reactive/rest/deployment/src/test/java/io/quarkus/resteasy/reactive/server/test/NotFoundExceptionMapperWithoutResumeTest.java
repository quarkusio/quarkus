package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.get;

import java.util.function.Supplier;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.ext.web.Router;

/**
 * Without {@code quarkus.rest.resume-on404}, a global exception mapper that matches {@code NotFoundException}
 * handles the paths that are not matched by any resource.
 */
public class NotFoundExceptionMapperWithoutResumeTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, CustomRoute.class, ThrowableExceptionMapper.class);
                }
            });

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
    public void lateCustomRouteIsHandledByTheMapper() {
        get("/late")
                .then()
                .statusCode(418);
    }

    @Test
    public void missingIsHandledByTheMapper() {
        get("/dummy")
                .then()
                .statusCode(418);
    }

    @Path("/test")
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
            router.get("/late").order(Integer.MAX_VALUE).handler(rc -> rc.response().end("late"));
        }
    }

    public static class ThrowableExceptionMapper {

        @ServerExceptionMapper
        public Response handleThrowable(Throwable t) {
            return Response.status(418).build();
        }
    }
}
