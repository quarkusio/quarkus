package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class ResourceMethodSameSignatureTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(GreetingResource.class));

    @Test
    void basicTest() {
        RestAssured.get("/greetings/Quarkus")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Quarkus"));

        RestAssured.get("/greetings/Quarkus/with-question")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello [Quarkus], how are you?"));

        RestAssured.given().contentType("text/plain").post("/greetings/Quarkus")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Quarkus"));

        RestAssured.given().contentType("application/xml").post("/greetings/Quarkus")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello [Quarkus], how are you?"));
    }

    @Path("/greetings")
    @ApplicationScoped
    public static class GreetingResource {

        @GET
        @Path("/{name}")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(@PathParam("name") String name) {
            return "Hello %s".formatted(name);
        }

        @GET
        @Path("/{name}/with-question")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(@PathParam("name") List<String> name) {
            return "Hello %s, how are you?".formatted(name);
        }

        @POST
        @Path("/{name}")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(@PathParam("name") String name, String ignored) {
            return "Hello %s".formatted(name);
        }

        @POST
        @Path("/{name}")
        @Consumes(MediaType.APPLICATION_XML)
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(@PathParam("name") List<String> name, String ignored) {
            return "Hello %s, how are you?".formatted(name);
        }
    }
}
