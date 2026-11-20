package io.quarkus.resteasy.reactive.server.test.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class NumberTextPlainBodyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Resource.class));

    @Test
    public void testWrapperBody() {
        given().contentType("text/plain").body("42")
                .post("/number/wrapper").then().statusCode(200).body(is("42"));
        given().contentType("text/plain").body("abc")
                .post("/number/wrapper").then().statusCode(400);
    }

    @Test
    public void testPrimitiveBody() {
        given().contentType("text/plain").body("42")
                .post("/number/primitive").then().statusCode(200).body(is("42"));
        given().contentType("text/plain").body("abc")
                .post("/number/primitive").then().statusCode(400);
    }

    @Path("/number")
    public static class Resource {

        @POST
        @Path("/wrapper")
        @Consumes(MediaType.TEXT_PLAIN)
        public String wrapper(Integer value) {
            return value.toString();
        }

        @POST
        @Path("/primitive")
        @Consumes(MediaType.TEXT_PLAIN)
        public String primitive(int value) {
            return Integer.toString(value);
        }
    }
}
