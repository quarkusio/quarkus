package io.quarkus.jwt.test;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RequiredClaimsUnitTest {
    private static Class[] testClasses = {
            RequiredClaimsEndpoint.class,
            TokenUtils.class
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
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem")
                    .addAsResource("RequiredClaims.json")
                    .addAsResource("application.properties"));

    @BeforeEach
    public void generateToken() throws Exception {
        HashMap<String, Long> timeClaims = new HashMap<>();
        token = TokenUtils.generateTokenString("/RequiredClaims.json", null, timeClaims);
        iatClaim = timeClaims.get(Claims.iat.name());
        authTimeClaim = timeClaims.get(Claims.auth_time.name());
        expClaim = timeClaims.get(Claims.exp.name());
    }

    /**
     * Verify that the token issuer claim is as expected
     *
     */
    @Test()
    public void verifyIssuerClaim() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.iss.name(), "https://server.example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyIssuer").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    /**
     * Verify that the token sub claim is as expected
     *
     */
    @Test()
    public void verifySubClaim() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.sub.name(), "24400320")
                .queryParam(Claims.iss.name(), "https://server.example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifySUB").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    /**
     * Verify that the token jti claim is as expected
     *
     */
    @Test()
    public void verifyJTI() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.jti.name(), "a-f2b2180c")
                .queryParam(Claims.iss.name(), "https://server.example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyJTI").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    /**
     * Verify that the token upn claim is as expected
     *
     */
    @Test()
    public void verifyUPN() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.upn.name(), "jdoe@example.com")
                .queryParam(Claims.iss.name(), "https://server.example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyUPN").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    /**
     * Verify that the token aud claim is as expected
     *
     */
    @Test()
    public void verifyAudience() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.aud.name(), "")
                .queryParam(Claims.iss.name(), "https://server.example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyAudience").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    /**
     * Verify that the token aud claim is as expected
     *
     */
    @Test()
    public void verifyAudience2() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.aud.name(), "")
                .queryParam(Claims.iss.name(), "https://server.example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyOptionalAudience").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    /**
     * Verify that the token iat claim is as expected
     *
     */
    @Test()
    public void verifyIssuedAt() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.iat.name(), iatClaim)
                .queryParam(Claims.iss.name(), "https://server.example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyIssuedAt").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    /**
     * Verify that the token exp claim is as expected
     *
     */
    @Test()
    public void verifyExpiration() {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.exp.name(), expClaim)
                .queryParam(Claims.iss.name(), "https://server.example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyExpiration").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
}
