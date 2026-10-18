package io.quarkus.resteasy.reactive.server.test.security;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A response mapped from an exception thrown by the security check, which runs before the
 * interceptors are set up for the request, must still go through the writer interceptors.
 */
public class SecurityFailureWriterInterceptorTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, CustomExceptionMappers.class, HeaderWriterInterceptor.class));

    @Test
    public void writerInterceptorRunsForSecurityFailure() {
        when().get("hello/secured")
                .then()
                .statusCode(401)
                .header("X-Writer-Interceptor", is("true"))
                .body(is("unauthorized"));
    }

    @Test
    public void writerInterceptorRunsForExceptionFromMethod() {
        when().get("hello/throwing")
                .then()
                .statusCode(401)
                .header("X-Writer-Interceptor", is("true"))
                .body(is("unauthorized"));
    }

    @Test
    public void writerInterceptorRunsForNormalResponse() {
        when().get("hello")
                .then()
                .statusCode(200)
                .header("X-Writer-Interceptor", is("true"))
                .body(is("hello world"));
    }

    @Path("hello")
    public static final class HelloResource {

        @GET
        public String hello() {
            return "hello world";
        }

        @GET
        @Path("secured")
        @RolesAllowed("test")
        public String secured() {
            return "secured";
        }

        @GET
        @Path("throwing")
        public String throwing() {
            throw new UnauthorizedException();
        }
    }

    public static final class CustomExceptionMappers {

        @ServerExceptionMapper(UnauthorizedException.class)
        public Response unauthorized() {
            return Response.status(401).entity("unauthorized").build();
        }
    }

    @Provider
    public static final class HeaderWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
            context.getHeaders().putSingle("X-Writer-Interceptor", "true");
            context.proceed();
        }
    }
}
