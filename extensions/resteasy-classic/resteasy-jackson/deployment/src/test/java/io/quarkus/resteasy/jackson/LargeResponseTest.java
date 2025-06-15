package io.quarkus.resteasy.jackson;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class LargeResponseTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(LargeResource.class));

    @Test
    public void testLargeResponseMultipleOfBuffer() {
        RestAssured.get("/large/bufmult").then().statusCode(200).body("key500", equalTo("value500"));
    }

    @Test
    public void testLargeResponse() {
        RestAssured.get("/large/huge").then().statusCode(200).body("key500", equalTo("value500"));
    }

}
