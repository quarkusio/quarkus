package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.Headers;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ValidNonBlockingFiltersTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(StandardBlockingRequestFilter.class, AnotherStandardBlockingRequestFilter.class,
                                    StandardNonBlockingRequestFilter.class, DummyResource.class);
                }
            });

    @Test
    public void testBlockingEndpoint() {
        Headers headers = RestAssured.given().get("/dummy/blocking")
                .then().statusCode(200).extract().headers();
        assertEquals(
                "1-custom-non-blocking/2-another-custom-non-blocking/3-standard-non-blocking/4-standard-blocking/5-another-standard-blocking/6-custom-blocking",
                headers.get("filter-request").getValue());
        assertEquals(
                "false/false/false/true/true/true",
                headers.get("thread").getValue());
    }

    @Test
    public void testNonBlockingEndpoint() {
        Headers headers = RestAssured.given().get("/dummy/nonblocking")
                .then().statusCode(200).extract().headers();
        assertEquals(
                "1-custom-non-blocking/2-another-custom-non-blocking/3-standard-non-blocking/4-standard-blocking/5-another-standard-blocking/6-custom-blocking",
                headers.get("filter-request").getValue());
        assertEquals(
                "false/false/false/false/false/false",
                headers.get("thread").getValue());
    }

    @Path("dummy")
    public static class DummyResource {

        @Blocking
        @Path("blocking")
        @GET
        public Response blocking(@Context HttpHeaders headers) {
            return getResponse(headers);
        }

        @Path("nonblocking")
        @GET
        public Uni<Response> nonblocking(@Context HttpHeaders headers) {
            return Uni.createFrom().item(getResponse(headers));
        }

        private Response getResponse(HttpHeaders headers) {
            return Response.ok()
                    .header("filter-request", headers.getHeaderString("filter-request"))
                    .header("thread", headers.getHeaderString("thread"))
                    .build();
        }
    }

    @Provider
    @Priority(Priorities.USER + 100)
    public static class StandardBlockingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            String previousFilterHeaderValue = headers.getFirst("filter-request");
            headers.putSingle("filter-request", previousFilterHeaderValue + "/4-standard-blocking");
            String previousThreadHeaderValue = headers.getFirst("thread");
            headers.putSingle("thread", previousThreadHeaderValue + "/" + BlockingOperationSupport.isBlockingAllowed());
        }
    }

    @Provider
    @Priority(Priorities.USER + 200)
    public static class AnotherStandardBlockingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            String previousFilterHeaderValue = headers.getFirst("filter-request");
            headers.putSingle("filter-request", previousFilterHeaderValue + "/5-another-standard-blocking");
            String previousThreadHeaderValue = headers.getFirst("thread");
            headers.putSingle("thread", previousThreadHeaderValue + "/" + BlockingOperationSupport.isBlockingAllowed());
        }
    }

    @Provider
    @Priority(Priorities.USER + 50)
    @NonBlocking
    public static class StandardNonBlockingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            String previousFilterHeaderValue = headers.getFirst("filter-request");
            headers.putSingle("filter-request", previousFilterHeaderValue + "/3-standard-non-blocking");
            String previousThreadHeaderValue = headers.getFirst("thread");
            headers.putSingle("thread", previousThreadHeaderValue + "/" + BlockingOperationSupport.isBlockingAllowed());
        }
    }

    public static class CustomFilters {

        @ServerRequestFilter(nonBlocking = true)
        public void firstNonBlocking(ContainerRequestContext requestContext) {
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            String previousFilterHeaderValue = headers.getFirst("filter-request");
            headers.putSingle("filter-request", "1-custom-non-blocking");
            headers.putSingle("thread", "" + BlockingOperationSupport.isBlockingAllowed());
        }

        @ServerRequestFilter(nonBlocking = true, priority = Priorities.USER + 20)
        public void anotherNonBlocking(ContainerRequestContext requestContext) {
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            String previousFilterHeaderValue = headers.getFirst("filter-request");
            headers.putSingle("filter-request",
                    previousFilterHeaderValue + "/2-another-custom-non-blocking");
            String previousThreadHeaderValue = headers.getFirst("thread");
            headers.putSingle("thread", previousThreadHeaderValue + "/" + BlockingOperationSupport.isBlockingAllowed());
        }

        @ServerRequestFilter(priority = Priorities.USER + 300)
        public void blocking(ContainerRequestContext requestContext) {
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            String previousFilterHeaderValue = headers.getFirst("filter-request");
            headers.putSingle("filter-request", previousFilterHeaderValue + "/6-custom-blocking");
            String previousThreadHeaderValue = headers.getFirst("thread");
            headers.putSingle("thread", previousThreadHeaderValue + "/" + BlockingOperationSupport.isBlockingAllowed());
        }
    }

}
