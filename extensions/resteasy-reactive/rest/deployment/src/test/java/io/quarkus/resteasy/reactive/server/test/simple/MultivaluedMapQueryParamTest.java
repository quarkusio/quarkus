package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.TreeMap;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class MultivaluedMapQueryParamTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(HelloResource.class));

    @Test
    public void noQueryParams() {
        RestAssured.get("/hello")
                .then().statusCode(200).body(Matchers.equalTo(""));
    }

    @Test
    public void singleQueryParam() {
        RestAssured.get("/hello?name=foo")
                .then().statusCode(200).body(Matchers.equalTo("name=[foo]"));
    }

    @Test
    public void multipleDifferentQueryParams() {
        RestAssured.get("/hello?name=foo&city=bar")
                .then().statusCode(200).body(Matchers.equalTo("city=[bar];name=[foo]"));
    }

    @Test
    public void multipleValuesForSameQueryParam() {
        RestAssured.get("/hello?name=foo&name=bar")
                .then().statusCode(200).body(Matchers.equalTo("name=[foo, bar]"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String hello(@RestQuery MultivaluedMap<String, String> queryParams) {
            return new TreeMap<>(queryParams).entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((a, b) -> a + ";" + b)
                    .orElse("");
        }
    }
}
