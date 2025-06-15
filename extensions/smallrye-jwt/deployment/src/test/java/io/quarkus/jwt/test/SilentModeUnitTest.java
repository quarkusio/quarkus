package io.quarkus.jwt.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SilentModeUnitTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(DefaultGroupsEndpoint.class).addAsResource("publicKey.pem")
                    .addAsResource("applicationJwtFormAuth.properties", "application.properties"));

    @Test
    public void testFormChallengeWithoutAuthorizationHeader() {
        RestAssured.given().redirects().follow(false).get("/endp/echo").then().assertThat().statusCode(302);
    }

    @Test
    public void testJwtChallengeWithAuthorizationHeader() {
        RestAssured.given().auth().oauth2("123").get("/endp/routingContext").then().assertThat().statusCode(401);
    }
}
