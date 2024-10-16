package io.quarkus.resteasy.reactive.jackson.deployment.test.response;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ResponseStringNonAsciiTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/hello")
                .then().statusCode(200)
                .and().body(Matchers.equalTo("{\"message\": \"Καλημέρα κόσμε\"}"))
                .and().contentType("application/json");
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response hello() {
            return Response.ok("{\"message\": \"Καλημέρα κόσμε\"}").build();
        }
    }
}
