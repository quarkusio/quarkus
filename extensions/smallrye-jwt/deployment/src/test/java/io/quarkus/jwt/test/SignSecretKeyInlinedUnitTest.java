package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

public class SignSecretKeyInlinedUnitTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultGroupsEndpoint.class)
                    .addAsResource("secretKey.jwk")
                    .addAsResource("applicationSignSecretKeyInlined.properties", "application.properties"));

    @Test
    public void echoGroups() {
        String token = Jwt.upn("upn").groups("User").sign();
        RestAssured.given().auth()
                .oauth2(token)
                .get("/endp/echo")
                .then().assertThat().statusCode(200)
                .body(equalTo("User"));
    }
}
