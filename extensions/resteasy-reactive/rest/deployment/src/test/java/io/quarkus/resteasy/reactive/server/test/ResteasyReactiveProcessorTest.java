package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.given;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ResteasyReactiveProcessorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(ResteasyReactiveProcessorTest.TestResource.class,
                    ResteasyReactiveProcessorTest.TestSubResource.class));

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
        @GET
        public String hello() {
            return "test/subresource";
        }
    }

    @Test
    public void testSimpleSubresource() {
        given().when().get("/test/subresource")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("test/subresource"));
    }

}
