package io.quarkus.jwt.test;

import java.io.StringReader;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class ScopingUnitTest {
    private static Class<?>[] testClasses = {
            DefaultScopedEndpoint.class,
            RequestScopedEndpoint.class,
            TokenUtils.class
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem")
                    .addAsResource("Token1.json")
                    .addAsResource("Token2.json")
                    .addAsResource("application.properties"));

    @Test
    public void verifyUsernameClaim() throws Exception {
        String token = TokenUtils.generateTokenString("/Token1.json");
        Response response = RestAssured.given().auth()
                .oauth2(token)
                .when()
                .queryParam("username", "jdoe")
                .get("/endp-defaultscoped/validateUsername").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assertions.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));

        String token2 = TokenUtils.generateTokenString("/Token2.json");
        Response response2 = RestAssured.given().auth()
                .oauth2(token2)
                .when()
                // We expect the injected preferred_username claim to still be jdoe due to default scope = @ApplicationScoped
                .queryParam("username", "jdoe")
                .get("/endp-defaultscoped/validateUsername").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response2.getStatusCode());
        String replyString2 = response2.body().asString();
        JsonReader jsonReader2 = Json.createReader(new StringReader(replyString2));
        JsonObject reply2 = jsonReader2.readObject();
        Assertions.assertTrue(reply2.getBoolean("pass"), reply2.getString("msg"));

        Response response3 = RestAssured.given().auth()
                .oauth2(token)
                .when()
                // We expect
                .queryParam("username", "jdoe")
                .get("/endp-requestscoped/validateUsername").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response3.getStatusCode());
        String replyString3 = response3.body().asString();
        JsonReader jsonReader3 = Json.createReader(new StringReader(replyString3));
        JsonObject reply3 = jsonReader3.readObject();
        Assertions.assertTrue(reply3.getBoolean("pass"), reply3.getString("msg"));

        Response response4 = RestAssured.given().auth()
                .oauth2(token2)
                .when()
                // Now we expect the injected claim to match the current caller
                .queryParam("username", "jdoe2")
                .get("/endp-requestscoped/validateUsername").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response4.getStatusCode());
        String replyString4 = response4.body().asString();
        JsonReader jsonReader4 = Json.createReader(new StringReader(replyString4));
        JsonObject reply4 = jsonReader4.readObject();
        Assertions.assertTrue(reply4.getBoolean("pass"), reply4.getString("msg"));
        Assertions.assertEquals("Bearer", reply4.getString("authScheme"));
    }
}
