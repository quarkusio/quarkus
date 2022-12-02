package io.quarkus.resteasy.test;

import static io.restassured.RestAssured.given;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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
