package io.quarkus.it.keycloak;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class OidcClientTest {

    @InjectWireMock
    WireMockServer server;

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
        waitUntillAccessTokenHasExpired();

        // access_token_1 has expired, refresh_token_1 is assumed to be valid and used to acquire access_token_2 and refresh_token_2.
        // access_token_2 expires in 4 seconds, but refresh_token_2 - in 1 sec - it will expire by the time access_token_2 has expired
        // "Default OidcClient has refreshed the tokens" record is added to the log
        RestAssured.when().get("/frontend/echoToken")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_2"));

        // Wait until the access_token_2 has expired
        waitUntillAccessTokenHasExpired();

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

    private static void waitUntillAccessTokenHasExpired() {
        long expiredTokenTime = System.currentTimeMillis() + 5000;
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(3))
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return System.currentTimeMillis() > expiredTokenTime;
                    }
                });
    }

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

    @Test
    public void testEchoTokensNonStandardResponse() {
        RestAssured.when().get("/frontend/echoTokenNonStandardResponse")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_n refresh_token_n"));
    }

    @Test
    public void testEchoTokensNonStandardResponseWithoutHeader() {
        RestAssured.when().get("/frontend/echoTokenNonStandardResponseWithoutHeader")
                .then()
                .statusCode(401);
    }

    @Test
    public void testEchoTokensRefreshTokenOnly() {
        RestAssured.given().queryParam("refreshToken", "shared_refresh_token")
                .when().get("/frontend/echoRefreshTokenOnly")
                .then()
                .statusCode(200)
                .body(equalTo("temp_access_token"));
    }

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
                        Assertions.assertTrue(Files.exists(accessLogFilePath),
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
