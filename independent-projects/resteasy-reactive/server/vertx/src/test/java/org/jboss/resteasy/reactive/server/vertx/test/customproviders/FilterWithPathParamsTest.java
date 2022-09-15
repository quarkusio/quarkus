package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class FilterWithPathParamsTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class, Filters.class);
                }
            });

    @Test
    public void testNonExistingPath() {
        when().get("/dummy")
                .then()
                .statusCode(404)
                .header("path-params", is("0"));
    }

    @Test
    public void testNoPathParamsPathNoAbort() {
        when().get("/hello")
                .then()
                .statusCode(200)
                .header("path-params", is("0"));
    }

    @Test
    public void testNoPathParamsPathWithAbort() {
        given().header("abort", "true")
                .when().get("/hello")
                .then()
                .statusCode(401)
                .header("path-params", is("0"));
    }

    @Test
    public void testPathParamsPathNoAbort() {
        when().get("/hello/resteasy")
                .then()
                .statusCode(200)
                .header("path-params", is("1"));
    }

    @Test
    public void testPathParamsPathWithAbort() {
        given().header("abort", "true")
                .when().get("/hello/resteasy")
                .then()
                .statusCode(401)
                .header("path-params", is("1"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello";
        }

        @Path("{name}")
        @GET
        public String helloName(String name) {
            return name;
        }
    }

    public static class Filters {

        @ServerRequestFilter
        public Uni<Response> filter(ResteasyReactiveContainerRequestContext requestContext) {
            if ("true".equals(requestContext.getHeaders().getFirst("abort"))) {
                requestContext.getUriInfo().getPathParameters();
                return Uni.createFrom().item(Response.status(401).build());
            }
            return null;
        }

        @ServerResponseFilter
        public void responseFilter(ResteasyReactiveContainerRequestContext requestContext,
                ContainerResponseContext responseContext) {
            responseContext.getHeaders().add("path-params", requestContext.getUriInfo().getPathParameters().size());
        }
    }
}
