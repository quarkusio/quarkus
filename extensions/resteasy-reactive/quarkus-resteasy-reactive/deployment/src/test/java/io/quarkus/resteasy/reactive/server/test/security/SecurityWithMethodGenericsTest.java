package io.quarkus.resteasy.reactive.server.test.security;

import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.providers.serialisers.ServerDefaultTextPlainBodyHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class SecurityWithMethodGenericsTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityProvider.class,
                            TestIdentityController.class,
                            BaseResource.class, AuthenticatedResource.class,
                            CustomServerDefaultTextPlainBodyHandler.class));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void test() {
        requestWithBasicAuth().get("/auth/allow").then().statusCode(200)
                .body(is("allow"));
        requestWithBasicAuth().contentType(MediaType.TEXT_PLAIN).body("12345")
                .post("/auth/generic").then()
                .statusCode(200);
        requestWithBasicAuth().contentType(MediaType.TEXT_PLAIN).body("54321").post("/auth/specific").then()
                .statusCode(200);
    }

    private RequestSpecification requestWithBasicAuth() {
        return RestAssured.given().auth().preemptive().basic("admin", "admin");
    }

    public static abstract class BaseResource<T> {

        @Path("generic")
        @POST
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.TEXT_PLAIN)
        public String generic(T body) {
            return "generic";
        }

        @Path("specific")
        @POST
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.TEXT_PLAIN)
        public String specific(String body) {
            return "specific";
        }
    }

    @Path("auth")
    @Authenticated
    public static class AuthenticatedResource extends BaseResource<String> {

        @Path("allow")
        @GET
        public String allow() {
            return "allow";
        }
    }

    @Provider
    @Consumes("text/plain")
    public static class CustomServerDefaultTextPlainBodyHandler extends ServerDefaultTextPlainBodyHandler {

        @Override
        public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        @Override
        public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            return "dummy";
        }
    }
}
