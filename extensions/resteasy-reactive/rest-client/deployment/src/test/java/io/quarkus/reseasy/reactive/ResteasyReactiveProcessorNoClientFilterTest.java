package io.quarkus.reseasy.reactive;

import static io.restassured.RestAssured.given;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ResteasyReactiveProcessorNoClientFilterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(ResteasyReactiveProcessorNoClientFilterTest.TestResource.class,
                    ResteasyReactiveProcessorNoClientFilterTest.TestSubResource.class));

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

    @Dependent
    public static class TestSubResource {

        @RestClient
        TestClient client;

        @GET
        public String hello() {
            return "test/subresource";
        }
    }

    @RegisterRestClient(baseUri = "test")
    public interface TestClient {
        @GET
        String hello();
    }

    @Test
    public void testSimpleSubresourceWithNoClientImportsOnClassLevel() {
        given().when().get("/test/subresource")
                .then()
                .statusCode(200);
    }

}
