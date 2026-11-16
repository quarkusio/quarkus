package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.get;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
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

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.resteasy.reactive.server.spi.ResumeOn404BuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.ext.web.Router;

public class ResumeOn404BuildItemTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, CustomRoute.class, ThrowableExceptionMapper.class);
                }
            })
            .addBuildChainCustomizer(buildCustomizer());

    protected static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<>() {
            // This represents the extension.
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(context -> {
                    context.produce(new ResumeOn404BuildItem());
                })
                        .produces(ResumeOn404BuildItem.class)
                        .build();
            }
        };
    }

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

    /**
     * A route with a lower priority than the REST routes, such as the welcome page in dev mode.
     */
    @Test
    public void matchingFromLateCustomRoute() {
        get("/late")
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
            router.get("/late").order(Integer.MAX_VALUE).handler(rc -> rc.response().end("late"));
        }
    }

    /**
     * A global mapper that also matches {@code NotFoundException} must not prevent the resume.
     */
    public static class ThrowableExceptionMapper {

        @ServerExceptionMapper
        public Response handleThrowable(Throwable t) {
            return Response.status(418).build();
        }
    }
}
