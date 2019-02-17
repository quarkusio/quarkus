package org.jboss.shamrock.jwt.test;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import io.restassured.RestAssured;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that claims can be injected as primitive types into @RequestScoped beans
 */
public class PrimitiveInjectionUnitTest {
    private static Class[] testClasses = {
            PrimitiveInjectionEndpoint.class
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
    }

    /**
     * Verify that the token issuer claim is as expected
     *
     * @throws Exception
     */
    @Test()
    public void verifyIssuerClaim() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.iss.name(), "https://server.example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedIssuer").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    /**
     * Verify that the injected raw token claim is as expected
     *
     * @throws Exception
     */
    @Test()
    public void verifyInjectedRawToken() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.raw_token.name(), token)
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedRawToken").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    /**
     * Verify that the token jti claim is as expected
     *
     * @throws Exception
     */
    @Test()
    public void verifyInjectedJTI() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.jti.name(), "a-123")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedJTI").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        // TODO add proper assertion
        //System.out.println(reply.toString());

    }

    /**
     * Verify that the token upn claim is as expected
     *
     * @throws Exception
     */
    @Test()
    public void verifyInjectedUPN() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.upn.name(), "jdoe@example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedUPN").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        // TODO add proper assertion
        //System.out.println(reply.toString());
    }

    /**
     * Verify that the token aud claim is as expected
     *
     * @throws Exception
     */
    @Test()
    public void verifyInjectedAudience() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.aud.name(), new String[]{"s6BhdRkqt3"})
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedAudience").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        // TODO add proper assertion
        //System.out.println(reply.toString());
    }

    /**
     * Verify that the token aud claim is as expected
     *
     * @throws Exception
     */
    @Test()
    public void verifyInjectedGroups() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.groups.name(), new String[]{
                        "Echoer", "Tester", "group1", "group2"})
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedGroups").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        // TODO add proper assertion
        //System.out.println(reply.toString());
    }

    /**
     * Verify that the token iat claim is as expected
     *
     * @throws Exception
     */
    @Test()
    public void verifyInjectedIssuedAt() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.iat.name(), iatClaim)
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedIssuedAt").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        // TODO add proper assertion
        //System.out.println(reply.toString());
    }

    /**
     * Verify that the token exp claim is as expected
     *
     * @throws Exception
     */
    @Test()
    public void verifyInjectedExpiration() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.exp.name(), expClaim)
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedExpiration").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        // TODO add proper assertion
        //System.out.println(reply.toString());
    }

    /**
     * Verify that the token customString claim is as expected
     *
     * @throws Exception
     */
    @Test()
    public void verifyInjectedCustomString() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam("value", "customStringValue")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedCustomString").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        // TODO add proper assertion
        //System.out.println(reply.toString());
    }

    /**
     * Verify that the token customString claim is as expected
     *
     * @throws Exception
     */
    @Test()
    public void verifyInjectedCustomDouble() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam("value", 3.141592653589793d)
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedCustomDouble").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        // TODO add proper assertion
        //System.out.println(reply.toString());
    }
}
