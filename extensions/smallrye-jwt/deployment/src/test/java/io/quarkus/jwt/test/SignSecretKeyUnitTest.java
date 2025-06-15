package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

public class SignSecretKeyUnitTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(DefaultGroupsEndpoint.class).addAsResource("secretKey.jwk")
                    .addAsResource("applicationSignSecretKey.properties", "application.properties"));

    /**
     * Validate a request with MP-JWT without a 'groups' claim is successful due to the default value being provided in
     * the configuration
     */
    @Test
    public void echoGroups() {
        String token = Jwt.upn("upn").groups("User").sign();
        RestAssured.given().auth().oauth2(token).get("/endp/echo").then().assertThat().statusCode(200)
                .body(equalTo("User"));
    }
}
