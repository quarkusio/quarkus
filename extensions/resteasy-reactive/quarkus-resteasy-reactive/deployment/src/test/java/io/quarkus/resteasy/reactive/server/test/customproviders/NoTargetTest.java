package io.quarkus.resteasy.reactive.server.test.customproviders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Headers;

public class NoTargetTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class, ThrowingPreMatchFilter.class, DummyExceptionMapper.class);
                }
            });

    @Path("hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello";
        }
    }

    @Test
    public void test() {
        Headers headers = RestAssured.get("/hello")
                .then().statusCode(200).extract().headers();
        assertEquals("mapper", headers.get("source").getValue());
        assertEquals("NullValues", headers.get("resourceInfoClass").getValue());
    }

    public static class CustomResponseFilter {

        @ServerResponseFilter
        public void filter(ContainerResponseContext responseContext, ResourceInfo resourceInfo) {
            responseContext.getHeaders().add("resourceInfoClass", resourceInfo.getClass().getSimpleName());
        }
    }

    @PreMatching
    @Provider
    public static class ThrowingPreMatchFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            throw new DummyException();
        }
    }

    @Provider
    public static class DummyExceptionMapper implements ExceptionMapper<DummyException> {
        @Override
        public Response toResponse(DummyException exception) {
            return Response.ok().header("source", "mapper").build();
        }
    }

    public static class DummyException extends RuntimeException {
        public DummyException() {
            super("dummy");
            setStackTrace(new StackTraceElement[0]);
        }
    }
}
