package io.quarkus.jwt.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
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
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
        io.restassured.response.Response response = RestAssured.given()
                .header("Cookie", cookieName + "=" + token)
                .get("/endp/echo").andReturn();

        Assertions.assertEquals(200, response.getStatusCode());
        String replyString = response.body().asString();
        // The missing 'groups' claim's default value, 'User' is expected
        Assertions.assertEquals("User", replyString);
    }

    private void testBadResponse(String cookieName) {
        io.restassured.response.Response response = RestAssured.given()
                .header("Cookie", cookieName + "=" + token)
                .get("/endp/echo").andReturn();

        Assertions.assertEquals(401, response.getStatusCode());
    }

}
