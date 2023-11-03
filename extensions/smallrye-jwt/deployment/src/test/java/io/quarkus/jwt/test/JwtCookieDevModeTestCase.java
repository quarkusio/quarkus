package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class JwtCookieDevModeTestCase {

    private static Class<?>[] testClasses = {
            DefaultGroupsEndpoint.class,
            TokenUtils.class
    };
    /**
     * The test generated JWT token string
     */
    private String token;

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem")
                    .addAsResource("TokenNoGroups.json")
                    .addAsResource("applicationJwtCookieDev.properties", "application.properties"));

    @BeforeEach
    public void generateToken() throws Exception {
        token = TokenUtils.generateTokenString("/TokenNoGroups.json");
    }

    /**
     * Validate a request with MP-JWT token in a Cookie header is successful
     *
     * @throws Exception
     */
    @Test
    public void echoGroupsHotReplacement() throws Exception {
        testBadResponse("cookie_a");
        test.modifyResourceFile("application.properties", s -> s.replaceAll("#", ""));
        testOKResponse("cookie_a");

        testBadResponse("cookie_b");
        test.modifyResourceFile("application.properties", s -> s.replace("cookie_a", "cookie_b"));

        testOKResponse("cookie_b");
        testBadResponse("cookie_a");
    }

    private void testOKResponse(String cookieName) {
        RestAssured.given()
                .header("Cookie", cookieName + "=" + token)
                .get("/endp/echo")
                .then().assertThat().statusCode(200)
                .body(equalTo("User"));
    }

    private void testBadResponse(String cookieName) {
        RestAssured.given()
                .header("Cookie", cookieName + "=" + token)
                .get("/endp/echo")
                .then().assertThat().statusCode(401);
    }

}
