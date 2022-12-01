package io.quarkus.resteasy.test;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CookieTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SetCookieResource.class));

    @Test
    public void testSetMultipleCookieAsString() {
        HashMap<String, Object> cookies = new HashMap<>();

        cookies.put("c1", "c1");
        cookies.put("c2", "c2");
        cookies.put("c3", "c3");

        RestAssured.when().get("/set-cookies").then().cookies(cookies);
    }
}
