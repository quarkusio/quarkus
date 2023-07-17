package io.quarkus.jwt.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SmallRyeJwtDisabledTest {
    private static Class<?>[] testClasses = {
            DefaultGroupsEndpoint.class
    };
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("publicKey.pem")
                    .addAsResource("smallryeJwtDisabled.properties", "application.properties"));

    @Test
    public void serviceIsNotSecured() throws Exception {
        RestAssured.given().get("/endp/echo").then().assertThat().statusCode(403);
    }
}
