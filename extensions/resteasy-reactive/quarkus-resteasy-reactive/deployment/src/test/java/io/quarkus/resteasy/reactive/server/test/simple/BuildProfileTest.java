package io.quarkus.resteasy.reactive.server.test.simple;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.arc.properties.UnlessBuildProperty;
import io.quarkus.test.QuarkusUnitTest;

/**
 * The integration test for the support of build time conditions on JAX-RS resource classes.
 */
class BuildProfileTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(
                            ResourceTest1.class, ResourceTest2.class, ResponseFilter1.class, ResponseFilter2.class,
                            ResponseFilter3.class, ResponseFilter4.class, ResponseFilter5.class, ResponseFilter6.class,
                            Feature1.class, Feature2.class, DynamicFeature1.class, DynamicFeature2.class,
                            ExceptionMapper1.class, ExceptionMapper2.class))
            .overrideConfigKey("some.prop1", "v1")
            .overrideConfigKey("some.prop2", "v2");

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
                .header("X-RF-7", notNullValue())
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

    @IfBuildProfile("test")
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

    @IfBuildProfile("foo")
    @Path("rt-2")
    public static class ResourceTest2 {

        @GET
        @Path("ok")
        public String ok() {
            return "ok2";
        }
    }

    @IfBuildProperty(name = "some.prop1", stringValue = "v1") // will be enabled because the value matches
    @Provider
    public static class ResponseFilter1 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-1", "Value");
        }
    }

    @IfBuildProperty(name = "some.prop1", stringValue = "v") // won't be enabled because the value doesn't match
    @Provider
    public static class ResponseFilter2 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-2", "Value");
        }
    }

    @IfBuildProfile("test")
    @UnlessBuildProperty(name = "some.prop2", stringValue = "v1") // will be enabled because the value doesn't match
    @Provider
    public static class ResponseFilter3 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-3", "Value");
        }
    }

    @UnlessBuildProperty(name = "some.prop2", stringValue = "v2") // won't be enabled because the value matches
    @Provider
    public static class ResponseFilter4 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-4", "Value");
        }
    }

    @IfBuildProfile("test")
    @Provider
    public static class ResponseFilter5 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-5", "Value");
        }
    }

    @IfBuildProfile("bar")
    @Provider
    public static class ResponseFilter6 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-6", "Value");
        }
    }

    public static class ResponseFilter7 implements ContainerResponseFilter {

        private final String value;

        public ResponseFilter7(String value) {
            this.value = value;
        }

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("X-RF-7", value);
        }
    }

    @IfBuildProfile("test")
    @Provider
    public static class Feature1 implements Feature {

        @Override
        public boolean configure(FeatureContext context) {
            context.register(ResponseFilter3.class);
            return true;
        }
    }

    @UnlessBuildProfile("test")
    @Provider
    public static class Feature2 implements Feature {

        @Override
        public boolean configure(FeatureContext context) {
            context.register(ResponseFilter4.class);
            return true;
        }
    }

    @IfBuildProfile("test")
    @Provider
    public static class ExceptionMapper1 implements ExceptionMapper<RuntimeException> {

        @Override
        public Response toResponse(RuntimeException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE.getStatusCode()).build();
        }
    }

    @UnlessBuildProfile("test")
    @Provider
    public static class ExceptionMapper2 implements ExceptionMapper<UnsupportedOperationException> {

        @Override
        public Response toResponse(UnsupportedOperationException exception) {
            return Response.status(Response.Status.NOT_IMPLEMENTED.getStatusCode()).build();
        }
    }

    @IfBuildProfile("test")
    @Provider
    public static class DynamicFeature1 implements DynamicFeature {

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            context.register(ResponseFilter5.class);
            context.register(new ResponseFilter7("Value"));
        }
    }

    @IfBuildProfile("bar")
    @Provider
    public static class DynamicFeature2 implements DynamicFeature {

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            context.register(ResponseFilter6.class);
        }
    }
}
