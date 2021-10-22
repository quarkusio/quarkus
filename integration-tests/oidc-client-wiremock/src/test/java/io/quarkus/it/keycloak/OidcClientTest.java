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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class OidcClientTest {

    @Test
    public void testEchoAndRefreshTokens() {
        RestAssured.when().get("/frontend/echoToken")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_1"));

        // Wait until the access token has expired
        long expiredTokenTime = System.currentTimeMillis() + 5000;
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(3))
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return System.currentTimeMillis() > expiredTokenTime;
                    }
                });

        RestAssured.when().get("/frontend/echoToken")
                .then()
                .statusCode(200)
                .body(equalTo("access_token_2"));
        checkLog();
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
                        assertEquals(1, tokenAcquisitionCount,
                                "Log file must contain a single OidcClientImpl token acquisition confirmation");
                        assertEquals(1, tokenRefreshedCount,
                                "Log file must contain a single OidcClientImpl token refresh confirmation");
                    }
                });
    }
}
