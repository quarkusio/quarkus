package io.quarkus.resteasy.test.root;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * The integration test allowing to ensure that we can rely on {@link Application#getClasses()} to specify explicitly
 * the classes to use for the application.
 */
class ApplicationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            IResourceTest.class, ResourceInheritedInterfaceTest.class,
                            AResourceTest.class, ResourceInheritedClassTest.class,
                            ResourceTest1.class, ResourceTest2.class,
                            ResponseFilter1.class, ResponseFilter2.class,
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

    @DisplayName("Should access to path inherited from an interface")
    @Test
    void should_call_inherited_from_interface() {
        when()
                .get("/rt-i/ok")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(Matchers.is("ok-i"));
    }

    @DisplayName("Should access to path inherited from a class where method is implemented")
    @Test
    void should_call_inherited_from_class_implemented() {
        when()
                .get("/rt-a/ok-1")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(Matchers.is("ok-a-1"));
    }

    @DisplayName("Should access to path inherited from a class where method is overridden")
    @Test
    void should_call_inherited_from_class_overridden() {
        when()
                .get("/rt-a/ok-2")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(Matchers.is("ok-a-2"));
    }

    @Path("rt-i")
    public interface IResourceTest {

        @GET
        @Path("ok")
        String ok();
    }

    public static class ResourceInheritedInterfaceTest implements IResourceTest {

        @Override
        public String ok() {
            return "ok-i";
        }
    }

    @Path("rt-a")
    public abstract static class AResourceTest {

        @GET
        @Path("ok-1")
        public abstract String ok1();

        @GET
        @Path("ok-2")
        public String ok2() {
            return "ok-a";
        }
    }

    public static class ResourceInheritedClassTest extends AResourceTest {

        @Override
        public String ok1() {
            return "ok-a-1";
        }

        @Override
        public String ok2() {
            return "ok-a-2";
        }
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
                            ResourceInheritedInterfaceTest.class, ResourceInheritedClassTest.class,
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
