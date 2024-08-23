package io.quarkus.resteasy.test;

import static io.restassured.RestAssured.given;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoProducesValueTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class));

    @Test
    public void testSomething() {
        given().when().get("/test/hello")
                .then().statusCode(200);
    }

    @Produces
    @Path("test")
    public static class TestResource {

        @GET
        @Path("hello")
        public Response getHello() {
            return Response.ok().build();
        }
    }
}
