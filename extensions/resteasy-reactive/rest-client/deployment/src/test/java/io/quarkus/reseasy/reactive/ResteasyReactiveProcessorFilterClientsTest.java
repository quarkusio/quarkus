package io.quarkus.reseasy.reactive;

import static io.restassured.RestAssured.given;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ResteasyReactiveProcessorFilterClientsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(ResteasyReactiveProcessorFilterClientsTest.TestResource.class,
                    ResteasyReactiveProcessorFilterClientsTest.TestSubResource.class));

    @Path("test")
    public static class TestResource {

        @Inject
        TestSubResource subResource;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String test() {
            return "test";
        }

        @Path("subresource")
        public TestSubResource subResource() {
            return subResource;
        }
    }

    // should not happen that a subresource is also a rest client
    @Dependent
    @RegisterRestClient
    public static class TestSubResource {
        @GET
        public String hello() {
            return "test/subresource";
        }
    }

    @Test
    public void testSimpleSubresource() {
        given().when().get("/test/subresource")
                .then()
                .statusCode(405);
    }

}
