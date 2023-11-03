package io.quarkus.resteasy.test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class QueryParamTest {

    private static final String HELLO = "hello ";
    private static final String NOBODY = "nobody";
    private static final String ALBERT = "albert";
    private static final String AND = " and ";
    private static final String JOSE = "jose";
    private static final String NAME = "name";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MyResource.class));

    @Test
    public void testWithSomeNames() {
        Assertions.assertEquals(HELLO + ALBERT + AND + JOSE,
                RestAssured.given().queryParam(NAME, ALBERT, JOSE).get("/greetings").asString());
    }

    @Path("/greetings")
    public static class MyResource {

        @GET
        public String sayHello(@QueryParam("name") final Optional<List<String>> names) {
            return HELLO + names.map(l -> l.stream().collect(Collectors.joining(AND)))
                    .orElse(NOBODY);
        }
    }
}
