package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import static org.hamcrest.Matchers.containsString;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class MalformedInputTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(EchoResource.class, Payload.class);
                }
            });

    @Test
    public void malformedJsonIsBadRequest() {
        // invalid JSON: missing comma between the properties
        RestAssured.with().contentType(ContentType.JSON).body("{ \"name\": \"cheese\" \"price\": 10 }")
                .post("/echo")
                .then().statusCode(400);
    }

    @Test
    public void malformedJsonReportsParsingDetails() {
        // in dev and test mode the parsing failure details are included in the response
        RestAssured.with().contentType(ContentType.JSON).body("{ \"name\": \"cheese\" \"price\": 10 }")
                .post("/echo")
                .then().statusCode(400)
                .body("message", containsString("Invalid token"))
                .body("line", org.hamcrest.Matchers.notNullValue())
                .body("column", org.hamcrest.Matchers.notNullValue());
    }

    @Path("echo")
    public static class EchoResource {

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        public Payload echo(Payload payload) {
            return payload;
        }
    }

    public static class Payload {
        public String name;
        public int price;
    }
}
