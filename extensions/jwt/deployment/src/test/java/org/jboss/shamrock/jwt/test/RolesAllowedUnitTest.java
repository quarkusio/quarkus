package org.jboss.shamrock.jwt.test;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RolesAllowedUnitTest {
    private static Class[] testClasses = {
            RolesEndpoint.class
    };
    /**
     * The test generated JWT token string
     */
    private String token;
    // Time claims in the token
    private Long iatClaim;
    private Long authTimeClaim;
    private Long expClaim;

    @RegisterExtension
    static final ShamrockUnitTest config = new ShamrockUnitTest()
            .setArchiveProducer(() ->
                                        ShrinkWrap.create(JavaArchive.class)
                                                .addClasses(testClasses)
                                                .addAsManifestResource("microprofile-config.properties")
            );


    @BeforeEach
    public void generateToken() throws Exception {
        HashMap<String, Long> timeClaims = new HashMap<>();
        token = TokenUtils.generateTokenString("/Token1.json", null, timeClaims);
        iatClaim = timeClaims.get(Claims.iat.name());
        authTimeClaim = timeClaims.get(Claims.auth_time.name());
        expClaim = timeClaims.get(Claims.exp.name());
        System.out.printf("BeforeAll.generateToken, %s\n", token);
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
     * @throws Exception
     */
    @Test()
    public void callEchoBASIC() throws Exception {
        System.out.printf("Begin callEchoBASIC, token=%s\n", token);
        Response response = RestAssured.given().auth()
                .basic("jdoe@example.com", "password")
                .when()
                .queryParam("input", "hello")
                .get("/endp/echo").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusCode());
        String replyString = response.body().asString();
        System.out.println(replyString);
    }

    /**
     * Validate a request with MP-JWT succeeds with HTTP_OK, and replies with hello, user={token upn claim}
     * @throws Exception
     */
    @Test()
    public void callEcho() throws Exception {
        System.out.printf("Begin callEcho, token=%s\n", token);
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam("input", "hello")
                .get("/endp/echo").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        System.out.println(replyString);
        // Must return hello, user={token upn claim}
        Assertions.assertEquals(replyString, "hello, user=jdoe@example.com");
    }

    /**
     * Validate a request with MP-JWT but no associated role fails with HTTP_FORBIDDEN
     * @throws Exception
     */
    @Test()
    public void callEcho2() throws Exception {
        System.out.printf("Begin callEcho2, token=%s\n", token);
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam("input", "hello")
                .get("/endp/echo2").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
        String replyString = response.body().asString();
        System.out.println(replyString);
    }

    /**
     * Validate a request with MP-JWT is able to access checkIsUserInRole with HTTP_OK
     * @throws Exception
     */
    @Test()
    public void checkIsUserInRole() throws Exception {
        System.out.printf("Begin checkIsUserInRole, token=%s\n", token);
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/endp/checkIsUserInRole").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        System.out.println(replyString);
    }

    /**
     * Validate a request with MP-JWT Token2 fails to access checkIsUserInRole with HTTP_FORBIDDEN
     * @throws Exception
     */
    @Test()
    public void checkIsUserInRoleToken2() throws Exception {
        String token2 = TokenUtils.generateTokenString("/Token2.json");
        System.out.printf("Begin checkIsUserInRoleToken2, token=%s\n", token2);
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token2)
                .when()
                .get("/endp/checkIsUserInRole").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
        String replyString = response.body().asString();
        System.out.println(replyString);
    }

    /**
     * Validate a request with MP-JWT Token2 fails to access checkIsUserInRole with HTTP_FORBIDDEN
     * @throws Exception
     */
    @Test()
    public void echoNeedsToken2Role() throws Exception {
        String token2 = TokenUtils.generateTokenString("/Token2.json");
        System.out.printf("Begin echoNeedsToken2Role, token=%s\n", token2);
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token2)
                .when()
                .queryParam("input", "hello")
                .get("/endp/echoNeedsToken2Role").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        System.out.println(replyString);
    }

    /**
     * Validate a request with MP-JWT Token2 calling echo fails with HTTP_FORBIDDEN
     * @throws Exception
     */
    @Test()
    public void echoWithToken2() throws Exception {
        String token2 = TokenUtils.generateTokenString("/Token2.json");
        System.out.printf("Begin echoWithToken2, token=%s\n", token2);
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token2)
                .when()
                .queryParam("input", "hello")
                .get("/endp/echo").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
        String replyString = response.body().asString();
        System.out.println(replyString);
    }

    /**
     * Validate a request with MP-JWT SecurityContext.getUserPrincipal() is a JsonWebToken
     * @throws Exception
     */
    @Test()
    public void getPrincipalClass() throws Exception {
        System.out.printf("Begin getPrincipalClass, token=%s\n", token);
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/endp/getPrincipalClass").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        System.out.println(replyString);
        // Must return isJsonWebToken:true
        Assertions.assertEquals("isJsonWebToken:true", replyString);
    }

    /**
     * This test requires that the server provide a mapping from the group1 grant in the token to a Group1MappedRole
     * application declared role.
     * @throws Exception
     */
    @Test()
    public void testNeedsGroup1Mapping() throws Exception {
        System.out.printf("Begin testNeedsGroup1Mapping, token=%s\n", token);
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/endp/needsGroup1Mapping").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        System.out.println(replyString);
    }

    /**
     * Validate that accessing secured method has HTTP_OK and injected JsonWebToken principal
     * @throws Exception
     */
    @Test()
    public void getInjectedPrincipal() throws Exception {
        System.out.printf("Begin getInjectedPrincipal, token=%s\n", token);
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .get("/endp/getInjectedPrincipal").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        System.out.println(replyString);
        // Must return isJsonWebToken:true
        Assertions.assertEquals("isJsonWebToken:true", replyString);
    }
}
