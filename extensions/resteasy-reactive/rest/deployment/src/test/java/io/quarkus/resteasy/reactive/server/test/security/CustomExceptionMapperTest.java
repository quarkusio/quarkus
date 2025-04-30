package io.quarkus.resteasy.reactive.server.test.security;

import static io.restassured.RestAssured.when;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.test.QuarkusUnitTest;

public class CustomExceptionMapperTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, CustomExceptionMappers.class));

    @Test
    public void shouldDenyUnannotated() {
        when().get("hello")
                .then()
                .statusCode(999);
    }

    @Path("hello")
    @RolesAllowed("test")
    public static final class HelloResource {

        @GET
        public String hello() {
            return "hello world";
        }
    }

    public static final class CustomExceptionMappers {

        @ServerExceptionMapper(UnauthorizedException.class)
        public Response forbidden() {
            return Response.status(999).build();
        }
    }
}
