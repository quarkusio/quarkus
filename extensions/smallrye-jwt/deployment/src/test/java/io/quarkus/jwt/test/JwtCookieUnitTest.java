package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class JwtCookieUnitTest {
    private static Class<?>[] testClasses = {
            DefaultGroupsEndpoint.class,
            TokenUtils.class
    };
    /**
     * The test generated JWT token string
     */
    private String token;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem")
                    .addAsResource("TokenNoGroups.json")
                    .addAsResource("applicationJwtCookie.properties", "application.properties"));

    @BeforeEach
    public void generateToken() throws Exception {
        token = TokenUtils.generateTokenString(null, "kid", "/TokenNoGroups.json", null, null);
    }

    /**
     * Validate a request with MP-JWT token in a Cookie header is successful
     *
     */
    @Test
    public void echoGroups() {
        RestAssured.given()
                .header("Cookie", "cookie_a=" + token)
                .get("/endp/echo")
                .then().assertThat().statusCode(200)
                .body(equalTo("User"));
    }
}
