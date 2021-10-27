package org.jboss.resteasy.reactive.server.vertx.test.simple;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The integration test allowing to ensure that we can rely on {@link Application#getClasses()} to specify explicitly
 * the classes to use for the application.
 */
class ApplicationTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest runner = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(
                            ResourceTest1.class, ResourceTest2.class, ResponseFilter1.class, ResponseFilter2.class,
                            ResponseFilter3.class, ResponseFilter4.class, ResponseFilter5.class, ResponseFilter6.class,
                            Feature1.class, Feature2.class, DynamicFeature1.class, DynamicFeature2.class,
                            ExceptionMapper1.class, ExceptionMapper2.class, AppTest.class));

    @DisplayName("Should access to ok of resource 1 and provide a response with the expected headers")
    @Test
    void should_call_ok_of_resource_1() {
        when()
                .get("/rt-1/ok")
                .then()
                .header("X-RF-1", notNullValue())
                .header("X-RF-2", nullValue())
                .header("X-RF-3", notNullValue())
                .header("X-RF-4", nullValue())
                .header("X-RF-5", notNullValue())
                .header("X-RF-6", nullValue())
                .body(Matchers.is("ok1"));
    }

    @DisplayName("Should access to ko of resource 1 and call the expected exception mapper")
    @Test
    void should_call_ko_of_resource_1() {
        when()
                .get("/rt-1/ko")
                .then()
                .statusCode(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
    }

    @DisplayName("Should access to ok of resource 1 and provide a response with the expected headers")
    @Test
    void should_not_call_ok_of_resource_2() {
        when()
                .get("/rt-2/ok")
                .then()
                .statusCode(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
    }

    @Path("rt-1")
    public static class ResourceTest1 {

        @GET
        @Path("ok")
        public String ok() {
            return "ok1";
        }

        @GET
        @Path("ko")
        public String ko() {
            throw new UnsupportedOperationException();
        }
    }

    @Path("rt-2")
    public static class ResourceTest2 {

        @GET
        @Path("ok")
        public String ok() {
            return "ok2";
        }
    }

    @Provider
    public static class ResponseFilter1 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-1", "Value");
        }
    }

    @Provider
    public static class ResponseFilter2 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-2", "Value");
        }
    }

    @Provider
    public static class ResponseFilter3 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-3", "Value");
        }
    }

    @Provider
    public static class ResponseFilter4 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-4", "Value");
        }
    }

    @Provider
    public static class ResponseFilter5 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-5", "Value");
        }
    }

    @Provider
    public static class ResponseFilter6 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-6", "Value");
        }
    }

    @Provider
    public static class Feature1 implements Feature {

        @Override
        public boolean configure(FeatureContext context) {
            context.register(ResponseFilter3.class);
            return true;
        }
    }

    @Provider
    public static class Feature2 implements Feature {

        @Override
        public boolean configure(FeatureContext context) {
            context.register(ResponseFilter4.class);
            return true;
        }
    }

    @Provider
    public static class ExceptionMapper1 implements ExceptionMapper<RuntimeException> {

        @Override
        public Response toResponse(RuntimeException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE.getStatusCode()).build();
        }
    }

    @Provider
    public static class ExceptionMapper2 implements ExceptionMapper<UnsupportedOperationException> {

        @Override
        public Response toResponse(UnsupportedOperationException exception) {
            return Response.status(Response.Status.NOT_IMPLEMENTED.getStatusCode()).build();
        }
    }

    @Provider
    public static class DynamicFeature1 implements DynamicFeature {

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            context.register(ResponseFilter5.class);
        }
    }

    @Provider
    public static class DynamicFeature2 implements DynamicFeature {

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            context.register(ResponseFilter6.class);
        }
    }

    public static class AppTest extends Application {

        @Override
        public Set<Class<?>> getClasses() {
            return new HashSet<>(
                    Arrays.asList(
                            ResourceTest1.class, Feature1.class, ExceptionMapper1.class));
        }

        @Override
        public Set<Object> getSingletons() {
            return new HashSet<>(
                    Arrays.asList(
                            new ResponseFilter1(), new DynamicFeature1()));
        }
    }
}
