package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

// use method order because we need to keep this test class extendable
// changing the order and adding extra waiting can lead to extra token refresh
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class OidcClientTest {

    @InjectWireMock
    WireMockServer server;

    @Order(5)
    @Test
    public void testEchoTokensJwtBearerAuthenticationFromAdditionalAttrs() {
        RestAssured.when().get("/frontend/echoTokenJwtBearerAuthentication")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_jwt_bearer"));
    }

    @Order(8)
    @Test
    public void testEchoTokensJwtBearerAuthenticationFromFile() {
        RestAssured.when().get("/frontend/echoTokenJwtBearerAuthenticationFromFile")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_jwt_bearer"));
    }

    @Order(7)
    @Test
    public void testGetAccessTokenWithConfiguredExpiresIn() {
        Response r = RestAssured.when().get("/frontend/echoTokenConfiguredExpiresIn");
        assertEquals(200, r.statusCode());
        String[] data = r.body().asString().split(" ");
        assertEquals(2, data.length);
        assertEquals("access_token_without_expires_in", data[0]);

        long now = System.currentTimeMillis() / 1000;
        long expectedExpiresAt = now + 7;
        long accessTokenExpiresAt = Long.valueOf(data[1]);
        assertTrue(accessTokenExpiresAt >= expectedExpiresAt - 1
                && accessTokenExpiresAt <= expectedExpiresAt + 5);
    }

    @Order(9)
    @Test
    public void testEchoTokensJwtBearerGrant() {
        RestAssured.when().get("/frontend/echoTokenJwtBearerGrant")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_jwt_bearer_grant"));
    }

    @Order(11)
    @Test
    public void testEchoAndRefreshTokens() {
        // access_token_1 and refresh_token_1 are acquired using a password grant request.
        // access_token_1 expires in 4 seconds, refresh_token_1 has no lifespan limit as no `refresh_expires_in` property is returned.
        // "Default OidcClient has acquired the tokens" record is added to the log
        RestAssured.when().get("/frontend/echoToken")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_1"));

        // Wait until the access_token_1 has expired
        waitUntillAccessTokenHasExpired(5000);

        // access_token_1 has expired, refresh_token_1 is assumed to be valid and used to acquire access_token_2 and refresh_token_2.
        // access_token_2 expires in 4 seconds, but refresh_token_2 - in 1 sec - it will expire by the time access_token_2 has expired
        // "Default OidcClient has refreshed the tokens" record is added to the log
        RestAssured.when().get("/frontend/echoToken")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_2"));

        // Wait until the access_token_2 has expired
        waitUntillAccessTokenHasExpired(5000);

        // Both access_token_2 and refresh_token_2 have now expired therefore a password grant request is repeated,
        // as opposed to using a refresh token grant.
        // access_token_1 is returned again - as the same token URL and grant properties are used and Wiremock stub returns access_token_1
        // 2nd "Default OidcClient has acquired the tokens" record is added to the log
        RestAssured.when().get("/frontend/echoToken")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_1"));

        checkLog();
    }

    private static void waitUntillAccessTokenHasExpired(int expirationMs) {
        long expiredTokenTime = System.currentTimeMillis() + expirationMs;
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(3))
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return System.currentTimeMillis() > expiredTokenTime;
                    }
                });
    }

    /**
     * same logic than {@link #testEchoAndRefreshTokens()}, but with concurrency
     */
    @Order(10)
    @Test
    public void testEchoAndRefreshTokensWithConcurrency() {
        server.resetRequests(); // reset request counters

        // Given: the first call trigger the token retrieval
        // When: 2 concurrent requests trigger token retrieval
        IntStream.range(0, 2).parallel().forEach(i -> {
            RestAssured.when().get("/frontend/crashTest")
                    .then()
                    .statusCode(200)
                    .body(equalTo("access_token_1"));
        });
        // Then: only one token retrieval should be made
        server.verify(1, WireMock.postRequestedFor(urlEqualTo("/tokens-with-delay")));

        server.resetRequests(); // reset request counters

        // Given : the token access_token_1 expires
        waitUntillAccessTokenHasExpired(2000);

        // When : 2 concurrents requests until the refresh was made (access_token_2 comes from the refresh)
        IntStream.range(0, 2).parallel().forEach(i -> {
            RestAssured.when().get("/frontend/crashTest")
                    .then()
                    .statusCode(200)
                    .body(equalTo("access_token_2"));
        });
        // Then: only one token retrieval should be made
        server.verify(1, WireMock.postRequestedFor(urlEqualTo("/tokens-with-delay")));
    }

    @Order(2)
    @Test
    public void testEchoTokensPasswordGrantPublicClient() {
        RestAssured.when().get("/frontend/password-grant-public-client")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_public_client"));
        RestAssured.when().get("/frontend/password-grant-public-client")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_public_client"));
    }

    @Order(1)
    @Test
    public void testEchoTokensNonStandardResponse() {
        RestAssured.when().get("/frontend/echoTokenNonStandardResponse")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_n refresh_token_n"));
    }

    @Order(4)
    @Test
    public void testEchoTokensNonStandardResponseWithoutHeader() {
        RestAssured.when().get("/frontend/echoTokenNonStandardResponseWithoutHeader")
                .then()
                .statusCode(401);
    }

    @Order(3)
    @Test
    public void testEchoTokensRefreshTokenOnly() {
        RestAssured.given().queryParam("refreshToken", "shared_refresh_token")
                .when().get("/frontend/echoRefreshTokenOnly")
                .then()
                .statusCode(200)
                .body(equalTo("temp_access_token"));
    }

    @Order(6)
    @Test
    public void testCibaGrant() {
        RestAssured.given().queryParam("authReqId", "16cdaa49-9591-4b63-b188-703fa3b25031")
                .when().get("/frontend/ciba-grant")
                .then()
                .statusCode(400)
                .body(equalTo("{\"error\":\"expired_token\"}"));

        server.setScenarioState("auth-device-approval", CibaAuthDeviceApprovalState.PENDING.name());

        RestAssured.given().queryParam("authReqId", "b1493f2f-c25c-40f5-8d69-94e2ad4b06df")
                .when().get("/frontend/ciba-grant")
                .then()
                .statusCode(400)
                .body(equalTo("{\"error\":\"authorization_pending\"}"));

        server.setScenarioState("auth-device-approval", CibaAuthDeviceApprovalState.DENIED.name());

        RestAssured.given().queryParam("authReqId", "b1493f2f-c25c-40f5-8d69-94e2ad4b06df")
                .when().get("/frontend/ciba-grant")
                .then()
                .statusCode(400)
                .body(equalTo("{\"error\":\"access_denied\"}"));

        server.setScenarioState("auth-device-approval", CibaAuthDeviceApprovalState.APPROVED.name());

        RestAssured.given().queryParam("authReqId", "b1493f2f-c25c-40f5-8d69-94e2ad4b06df")
                .when().get("/frontend/ciba-grant")
                .then()
                .statusCode(200)
                .body(equalTo("ciba_access_token"));

    }

    @Order(12)
    @Test
    public void testDeviceCodeGrant() {
        RestAssured.given().queryParam("deviceCode", "987654321")
                .when().get("/frontend/device-code-grant")
                .then()
                .statusCode(401);

        RestAssured.given().queryParam("deviceCode", "123456789")
                .when().get("/frontend/device-code-grant")
                .then()
                .statusCode(200)
                .body(equalTo("device_code_access_token"));

    }

    @Order(13)
    @Test
    public void testEchoWithRefreshInterval() {
        try {
            // make a call so that we know current token index
            String accessToken = RestAssured.when().get("/frontend/tokenRefreshInterval")
                    .then()
                    .statusCode(200)
                    .extract().asString();
            int tokenIndex = Integer.parseInt(accessToken.substring(accessToken.lastIndexOf('_') + 1));
            assertTrue(accessToken.startsWith("access_token_"));

            // wait until it is clear that token was refreshed again (wouldn't happen without configured refresh interval)
            waitUntillAccessTokenHasExpired(2000);

            Awaitility.await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> server.verify(new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN, 0),
                            WireMock.postRequestedFor(urlEqualTo("/tokens-refresh-test")).withRequestBody(
                                    matching(".*refresh_token=refresh_token_" + tokenIndex + ".*"))));

            // check that call is made with the refreshed token
            Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
                String accessToken2 = RestAssured.when().get("/frontend/tokenRefreshInterval")
                        .then()
                        .statusCode(200)
                        .extract().asString();
                assertTrue(accessToken2.startsWith("access_token_"));
                int nextTokenIndex = Integer.parseInt(accessToken2.substring(accessToken2.lastIndexOf('_') + 1));
                assertTrue(tokenIndex < nextTokenIndex);
            });
        } finally {
            server.resetRequests();
        }
    }

    private void checkLog() {
        final Path logDirectory = Paths.get(".", "target");
        given().await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        Path accessLogFilePath = logDirectory.resolve("quarkus.log");
                        boolean fileExists = Files.exists(accessLogFilePath);
                        if (!fileExists) {
                            accessLogFilePath = logDirectory.resolve("target/quarkus.log");
                            fileExists = Files.exists(accessLogFilePath);
                        }
                        assertTrue(Files.exists(accessLogFilePath),
                                "quarkus log file " + accessLogFilePath + " is missing");

                        int tokenAcquisitionCount = 0;
                        int tokenRefreshedCount = 0;

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(accessLogFilePath)),
                                        StandardCharsets.UTF_8))) {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains("Default OidcClient has refreshed the tokens")) {
                                    tokenRefreshedCount++;
                                } else if (line.contains("Default OidcClient has acquired the tokens")) {
                                    tokenAcquisitionCount++;
                                }

                            }
                        }
                        assertEquals(2, tokenAcquisitionCount,
                                "Log file must contain two OidcClientImpl token acquisition confirmations");
                        assertEquals(1, tokenRefreshedCount,
                                "Log file must contain a single OidcClientImpl token refresh confirmation");
                    }
                });
    }
}
