package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.function.Supplier;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.common.annotation.NonBlocking;

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
        @NonBlocking
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

        @JsonCreator
        public Greeting(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
