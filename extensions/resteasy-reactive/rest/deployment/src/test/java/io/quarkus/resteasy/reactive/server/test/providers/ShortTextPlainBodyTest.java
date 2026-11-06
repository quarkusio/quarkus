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

public class ShortTextPlainBodyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Resource.class));

    @Test
    public void testShortBody() {
        given().contentType("text/plain").body("42")
                .post("/short").then().statusCode(200).body(is("42"));
    }

    @Path("/short")
    public static class Resource {

        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        public String post(Short value) {
            return value.toString();
        }
    }
}
