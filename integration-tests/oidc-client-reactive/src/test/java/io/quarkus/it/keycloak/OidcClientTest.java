package io.quarkus.it.keycloak;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.containsString;
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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class OidcClientTest {

    @Test
    public void testGetUserNameReactive() {
        RestAssured.given().header("Accept", "text/plain")
                .when().get("/frontend/userNameReactive")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void testGetUserNameCustomFilter() {
        RestAssured.given().header("Accept", "text/plain")
                .when().get("/frontend/userNameCustomFilter")
                .then()
                .statusCode(200)
                .body(equalTo("jdoe"));
    }

    @Test
    public void testGetUserNameNamedFilter() {
        RestAssured.given().header("Accept", "text/plain")
                .when().get("/frontend/userNameNamedFilter")
                .then()
                .statusCode(200)
                .body(equalTo("jdoe"));
    }

    @Test
    public void testGetUserNameDisabledClient() {
        RestAssured.given().header("Accept", "text/plain")
                .when().get("/frontend/userNameDisabledClient")
                .then()
                .statusCode(200)
                .body(containsString("Unauthorized, status code 401"));
    }

    @Test
    public void testGetUserNameMisconfiguredClientFilter() {
        RestAssured.given().header("Accept", "text/plain")
                .when().get("/frontend/userNameMisconfiguredClientFilter")
                .then()
                .statusCode(200)
                .body(containsString("invalid_grant"));
    }

    @Test
    public void testGetUserNameReactiveAndRefreshTokens() {
        RestAssured.given().header("Accept", "text/plain")
                .when().get("/frontend/userNameReactive")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));

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

        RestAssured.given().header("Accept", "text/plain")
                .when().get("/frontend/userNameReactive")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
        checkLog();
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
                        int tokenRefreshedOidcClientLogCount = 0;
                        int tokenRefreshResponseFilterLogCount = 0;

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(accessLogFilePath)),
                                        StandardCharsets.UTF_8))) {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains("Default OidcClient has refreshed the tokens")) {
                                    tokenRefreshedOidcClientLogCount++;
                                } else if (line.contains("Default OidcClient has acquired the tokens")) {
                                    tokenAcquisitionCount++;
                                }
                                if (line.contains("Tokens have been refreshed")) {
                                    tokenRefreshResponseFilterLogCount++;
                                }

                            }
                        }
                        // blocking and reactive filters both acquire the tokens
                        assertEquals(1, tokenAcquisitionCount,
                                "Log file must contain a single OidcClientImpl token acquisition confirmation");
                        // only the reactive filter is refreshing the token
                        assertEquals(1, tokenRefreshedOidcClientLogCount,
                                "Log file must contain a single OidcClientImpl token refresh confirmation");

                        assertEquals(1, tokenRefreshResponseFilterLogCount,
                                "Log file must contain a single OidcResponseFilter token refresh confirmation");
                    }
                });
    }
}
