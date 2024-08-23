package io.quarkus.resteasy.reactive.jaxb.deployment.test;

import static org.hamcrest.Matchers.equalTo;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class EmptyInputTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class, Greeting.class));

    @Test
    public void emptyBlocking() {
        RestAssured.with().contentType(ContentType.XML).post("/greeting/blocking")
                .then().statusCode(200);
    }

    @Test
    public void emptyNonBlocking() {
        RestAssured.with().contentType(ContentType.XML).post("/greeting/nonBlocking")
                .then().statusCode(200);
    }

    @Test
    public void nonEmptyBlocking() {
        RestAssured.with().contentType(ContentType.XML).body(new Greeting("Hi")).post("/greeting/blocking")
                .then().statusCode(200).body(equalTo("Hi"));
    }

    @Test
    public void nonEmptyNonBlocking() {
        RestAssured.with().contentType(ContentType.XML).body(new Greeting("Hey")).post("/greeting/nonBlocking")
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

    @XmlRootElement
    public static class Greeting {

        @XmlElement
        private final String message;

        public Greeting(String message) {
            this.message = message;
        }

        private Greeting() {
            message = null;
        }

        public String getMessage() {
            return message;
        }

        /** Creates a new instance, will only be used by Jaxb. */
        private static Greeting newInstance() {
            return new Greeting();
        }
    }
}
