package io.quarkus.jwt.test;

import java.net.HttpURLConnection;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class RolesAllowedUnitTest {
    private static Class<?>[] testClasses = {
            RolesEndpoint.class,
            AuthenticatedEndpoint.class,
            PermitAllEndpoint.class,
            GreetingService.class,
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
                    .addAsResource("Token1.json")
                    .addAsResource("Token2.json")
                    .addAsResource("application.properties"));

    @BeforeEach
    public void generateToken() throws Exception {
        HashMap<String, Long> timeClaims = new HashMap<>();
        token = TokenUtils.generateTokenString("/Token1.json", null, timeClaims);
    }

    @Test()
    public void callEchoNoAuth() {
        RestAssured.given()
                .when()
                .queryParam("input", "hello")
                .get("/endp/echo")
                .then()
                .statusCode(HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test()
    public void testAuthenticatedAnnotation() {
        RestAssured.given()
                .when()
                .queryParam("input", "hello")
                .get("/endp/authenticated")
                .then()
                .statusCode(HttpURLConnection.HTTP_UNAUTHORIZED);

        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/endp/authenticated").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        Assertions.assertEquals("jdoe@example.com", replyString);
    }

    @Test()
    public void testAuthenticatedAnnotationOnClass() {
        RestAssured.given()
                .when()
                .queryParam("input", "hello")
                .get("/authenticated-endpoint/greet")
                .then()
                .statusCode(HttpURLConnection.HTTP_UNAUTHORIZED);

        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/authenticated-endpoint/greet").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        Assertions.assertEquals("hello", replyString);
    }

    @Test()
    public void testPermitAllOnClass() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/permit-all-endpoint/greet").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        Assertions.assertEquals("hello", replyString);
    }

    /**
     * Validate a request without an MP-JWT to unsecured endpoint has HTTP_OK with expected response
     */
    @Test()
    public void callHeartbeat() {
        RestAssured.given()
                .when()
                .queryParam("input", "hello")
                .get("/endp/heartbeat")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK);
    }

    /**
     * Verify that the injected authenticated principal is as expected
     *
     */
    @Test()
    public void callEchoBASIC() {
        Response response = RestAssured.given().auth()
                .basic("jdoe@example.com", "password")
                .when()
                .queryParam("input", "hello")
                .get("/endp/echo").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusCode());
    }

    /**
     * Validate a request with MP-JWT succeeds with HTTP_OK, and replies with hello, user={token upn claim}
     *
     */
    @Test()
    public void callEcho() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam("input", "hello")
                .get("/endp/echo").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        // Must return hello, user={token upn claim}
        Assertions.assertEquals(replyString, "hello, user=jdoe@example.com");
    }

    /**
     * Validate a request with MP-JWT but no associated role fails with HTTP_FORBIDDEN
     *
     */
    @Test()
    public void callEcho2() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam("input", "hello")
                .get("/endp/echo2").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
    }

    /**
     * Validate a request with MP-JWT is able to access checkIsUserInRole with HTTP_OK
     *
     */
    @Test()
    public void checkIsUserInRole() {

        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/endp/checkIsUserInRole").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        Assertions.assertEquals("jdoe@example.com", replyString);
    }

    /**
     * Validate a request with MP-JWT Token2 fails to access checkIsUserInRole with HTTP_FORBIDDEN
     *
     * @throws Exception
     */
    @Test()
    public void checkIsUserInRoleToken2() throws Exception {
        String token2 = TokenUtils.generateTokenString("/Token2.json");
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token2)
                .when()
                .get("/endp/checkIsUserInRole").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
        String replyString = response.body().asString();

        Assertions.assertEquals("", replyString);
    }

    /**
     * Validate a request with MP-JWT Token2 fails to access checkIsUserInRole with HTTP_FORBIDDEN
     *
     * @throws Exception
     */
    @Test()
    public void echoNeedsToken2Role() throws Exception {
        String input = "hello";
        String token2 = TokenUtils.generateTokenString("/Token2.json");
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token2)
                .when()
                .queryParam("input", input)
                .get("/endp/echoNeedsToken2Role").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        Assertions.assertEquals(input + ", user=jdoe2@example.com", replyString);
    }

    /**
     * Validate a request with MP-JWT Token2 calling echo fails with HTTP_FORBIDDEN
     *
     * @throws Exception
     */
    @Test()
    public void echoWithToken2() throws Exception {
        String token2 = TokenUtils.generateTokenString("/Token2.json");
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token2)
                .when()
                .queryParam("input", "hello")
                .get("/endp/echo").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
    }

    /**
     * Validate a request with MP-JWT SecurityContext.getUserPrincipal() is a JsonWebToken
     *
     */
    @Test()
    public void getPrincipalClass() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/endp/getPrincipalClass").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        // Must return isJsonWebToken:true
        Assertions.assertEquals("isJsonWebToken:true", replyString);
    }

    /**
     * This test requires that the server provide a mapping from the group1 grant in the token to a Group1MappedRole
     * application declared role.
     *
     */
    @Test()
    public void testNeedsGroup1Mapping() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/endp/needsGroup1Mapping").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        Assertions.assertEquals("jdoe@example.com", replyString);
    }

    /**
     * Validate that accessing secured method has HTTP_OK and injected JsonWebToken principal
     *
     */
    @Test()
    public void getInjectedPrincipal() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/endp/getInjectedPrincipal").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        // Must return isJsonWebToken:true
        Assertions.assertEquals("isJsonWebToken:true", replyString);
    }
}
