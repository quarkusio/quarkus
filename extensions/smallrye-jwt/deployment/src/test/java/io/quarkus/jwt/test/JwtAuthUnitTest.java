package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

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
import io.restassured.response.Response;

public class JwtAuthUnitTest {
    private static Class<?>[] testClasses = {
            JsonValuejectionEndpoint.class,
            TokenUtils.class
    };
    /**
     * The test generated JWT token string
     */
    private String token;
    // Time claims in the token
    private Long authTimeClaim;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem")
                    .addAsResource("Token1.json")
                    .addAsResource("application.properties"));

    @BeforeEach
    public void generateToken() throws Exception {
        HashMap<String, Long> timeClaims = new HashMap<>();
        token = TokenUtils.generateTokenString("/Token1.json", null, timeClaims);
        authTimeClaim = timeClaims.get(Claims.auth_time.name());
    }

    @Test()
    public void testSecureAccessFailure() {
        RestAssured.when().get("/endp/verifyInjectedIssuer").then()
                .statusCode(401)
                .header("WWW-Authenticate", equalTo("Bearer"));
    }

    /**
     * Verify that the injected token issuer claim is as expected
     *
     */
    @Test()
    public void verifyIssuerClaim() {
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
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
}
