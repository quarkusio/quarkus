package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.TreeMap;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class MultivaluedMapHeaderParamTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(HelloResource.class));

    @Test
    public void singleHeaderParam() {
        RestAssured.given().header("X-Custom-Name", "foo")
                .get("/hello")
                .then().statusCode(200).body(Matchers.equalTo("X-Custom-Name=[foo]"));
    }

    @Test
    public void multipleDifferentHeaderParams() {
        RestAssured.given().header("X-Custom-Name", "foo").header("X-Custom-City", "bar")
                .get("/hello")
                .then().statusCode(200).body(Matchers.equalTo("X-Custom-City=[bar];X-Custom-Name=[foo]"));
    }

    @Test
    public void multipleValuesForSameHeaderParam() {
        RestAssured.given().header("X-Custom-Name", "foo").header("X-Custom-Name", "bar")
                .get("/hello")
                .then().statusCode(200).body(Matchers.equalTo("X-Custom-Name=[foo, bar]"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String hello(@RestHeader MultivaluedMap<String, String> headers) {
            // filter out the headers that are always present on a real HTTP request (Host, Accept, User-Agent, ...)
            // and only keep the custom ones set by the test
            return new TreeMap<>(headers).entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("X-Custom"))
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((a, b) -> a + ";" + b)
                    .orElse("");
        }
    }
}
