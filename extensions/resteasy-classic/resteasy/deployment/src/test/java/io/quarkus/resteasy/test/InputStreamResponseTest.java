package io.quarkus.resteasy.test;

import static io.restassured.RestAssured.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InputStreamResponseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RootResource.class));

    @Test
    public void test() {
        when().get("/test").then().body(Matchers.is("Hello World"));
    }

    @Path("test")
    public static class TestResource {

        @Produces("text/plain")
        @GET
        public Response test() {
            return Response.ok(new ByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8))).build();
        }
    }
}
