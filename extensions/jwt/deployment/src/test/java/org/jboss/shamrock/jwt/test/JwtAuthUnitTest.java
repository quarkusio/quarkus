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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class JwtAuthUnitTest {
    private static Class[] testClasses = {
            JsonValuejectionEndpoint.class
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

    // Basic @ServletSecurity tests
    @Test()
    public void testSecureAccessFailure() {
        RestAssured.when().get("/endp/verifyInjectedIssuer").then()
                .statusCode(401);
    }

    /**
     * Verify that the injected token issuer claim is as expected
     * @throws Exception
     */
    @Test()
    public void verifyIssuerClaim() throws Exception {
        System.out.printf("Begin verifyIssuerClaim, token=%s\n", token);
        Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam(Claims.iss.name(), "https://server.example.com")
                .queryParam(Claims.auth_time.name(), authTimeClaim)
                .get("/endp/verifyInjectedIssuer").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        System.out.println(reply.toString());
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
}
