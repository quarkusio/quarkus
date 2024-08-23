package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.function.Supplier;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class EmptyInputTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(GreetingResource.class, Greeting.class);
                }
            });

    @Test
    public void emptyBlocking() {
        RestAssured.with().contentType(ContentType.JSON).post("/greeting/blocking")
                .then().statusCode(200).body(equalTo("null"));
    }

    @Test
    public void emptyNonBlocking() {
        RestAssured.with().contentType(ContentType.JSON).post("/greeting/nonBlocking")
                .then().statusCode(200).body(equalTo("null"));
    }

    @Test
    public void nonEmptyBlocking() {
        RestAssured.with().contentType(ContentType.JSON).body("{\"message\": \"Hi\"}").post("/greeting/blocking")
                .then().statusCode(200).body(equalTo("Hi"));
    }

    @Test
    public void nonEmptyNonBlocking() {
        RestAssured.with().contentType(ContentType.JSON).body("{\"message\": \"Hey\"}").post("/greeting/nonBlocking")
                .then().statusCode(200).body(equalTo("Hey"));
    }

    @Path("greeting")
    public static class GreetingResource {

        @Path("blocking")
        @POST
        public String blocking(Greeting greeting) {
            return createResult(greeting);
        }

        @Path("nonBlocking")
        @POST
        public String nonBlocking(Greeting greeting) {
            return createResult(greeting);
        }

        private String createResult(Greeting greeting) {
            if (greeting == null) {
                return "null";
            }
            return greeting.getMessage();
        }
    }

    public static class Greeting {

        private final String message;

        @JsonbCreator
        public Greeting(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
