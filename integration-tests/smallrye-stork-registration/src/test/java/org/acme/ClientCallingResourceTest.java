package org.acme;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ClientCallingResourceTest {

    @Test
    public void test() {
        Set<String> bodies = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            bodies.add(RestAssured.get("/api").then().statusCode(200).extract().body().asString());
        }
        Assertions.assertEquals(2, bodies.size());
    }

}
