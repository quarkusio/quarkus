package io.quarkus.oidc.client;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.client.runtime.OidcClientImpl;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTestResource(KeycloakRealmUserPasswordManager.class)
public class OidcClientUserPasswordTestCase {

    private static Class<?>[] testClasses = {
            OidcClientResource.class,
            OidcPublicClientResource.class,
            ProtectedResource.class
    };

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-oidc-client-user-password.properties", "application.properties"))
            .setLogRecordPredicate(r -> r.getLoggerName().equals(Logger.getLogger(OidcClientImpl.class).getName()))
            .assertLogRecords(r -> assertLogRecord(r));

    @Test
    public void testPasswordGrantToken() {
        String token = RestAssured.when().get("/client/token").body().asString();
        RestAssured.given().auth().oauth2(token)
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));

    }

    @Test
    public void testPasswordGrantTokenProvider() {
        String token = RestAssured.when().get("/client/tokenprovider").body().asString();
        RestAssured.given().auth().oauth2(token)
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));

    }

    @Test
    public void testPublicClientPasswordGrantToken() {
        String token = RestAssured.when().get("/public-client/token").body().asString();
        RestAssured.given().auth().oauth2(token)
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));

    }

    @Test
    public void testPasswordGrantTokens() {
        String[] tokens = RestAssured.when().get("/client/tokens").body().asString().split(" ");
        assertTokensNotNull(tokens);

        RestAssured.given().auth().oauth2(tokens[0])
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));

    }

    @Test
    public void testRefreshPasswordGrantTokens() {
        String[] tokens = RestAssured.when().get("/client/tokens").body().asString().split(" ");
        assertTokensNotNull(tokens);

        RestAssured.given().auth().oauth2(tokens[0])
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));

        // Wait until the access token has expired
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(1))
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Response r = RestAssured.given().auth().oauth2(tokens[0])
                                .when().get("/protected").andReturn();
                        return r.getStatusCode() == 401;
                    }
                });

        String[] refreshedTokens = RestAssured.given().queryParam("refreshToken", tokens[1])
                .when().get("/client/refresh-tokens").body().asString().split(" ");
        assertTokensNotNull(refreshedTokens);
        assertNotEquals(tokens[0], refreshedTokens[0]);

        RestAssured.given().auth().oauth2(refreshedTokens[0])
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    private static void assertTokensNotNull(String[] tokens) {
        assertEquals(2, tokens.length);
        assertNotNull(tokens[0]);
        assertNotNull(tokens[1]);
    }

    private static void assertLogRecord(List<LogRecord> records) {
        List<LogRecord> formRecords = records.stream()
                .filter(r -> (r.getMessage().contains("password=") || r.getMessage().contains("refresh_token=")))
                .collect(Collectors.toList());
        assertFalse(formRecords.isEmpty());

        List<LogRecord> passwordRecords = formRecords.stream().filter(r -> r.getMessage().contains("password="))
                .collect(Collectors.toList());
        assertFalse(passwordRecords.isEmpty());
        assertTrue(passwordRecords.stream().allMatch(r -> r.getMessage().contains("password=...")));

        List<LogRecord> refreshTokenRecords = formRecords.stream().filter(r -> r.getMessage().contains("refresh_token="))
                .collect(Collectors.toList());
        assertFalse(refreshTokenRecords.isEmpty());

        assertTrue(refreshTokenRecords.stream().allMatch(r -> r.getMessage().contains("refresh_token=...")));
    }
}
