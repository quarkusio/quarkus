package io.quarkus.it.bouncycastle;

import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.awaitility.core.ThrowingRunnable;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.util.JavaVersionUtil;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class BouncyCastleJsseTestCase {

    private static final Logger LOG = Logger.getLogger(BouncyCastleJsseTestCase.class);

    @TestHTTPResource(ssl = true)
    URL url;

    @Test
    public void testListProviders() {
        if (!JavaVersionUtil.isJava11OrHigher()) {
            LOG.trace("Skipping BouncyCastleJsseITCase, Java version is older than 11");
            return;
        }
        RequestSpecification spec = new RequestSpecBuilder()
                .setBaseUri(String.format("%s://%s", url.getProtocol(), url.getHost()))
                .setPort(url.getPort())
                .setKeyStore("client-keystore.jks", "password")
                .setTrustStore("client-truststore.jks", "password")
                .build();
        RestAssured.given()
                .spec(spec)
                .when()
                .get("/jsse/listProviders")
                .then()
                .statusCode(200)
                .body(containsString("BC,BCJSSE,SunJSSE"));
        checkLog();
    }

    private void checkLog() {
        final Path logDirectory = Paths.get(".", "target");
        given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        final Path accessLogFilePath = logDirectory.resolve("quarkus.log");
                        Assertions.assertTrue(Files.exists(accessLogFilePath),
                                "access log file " + accessLogFilePath + " is missing");

                        boolean checkClientPassed = false;
                        boolean checkServerPassed = false;

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(accessLogFilePath)),
                                        StandardCharsets.UTF_8))) {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                if (!checkServerPassed && line.contains("ProvTlsServer")
                                        && line.equals(
                                                "org.bouncycastle.jsse.provider.ProvTlsServer - Server selected protocol version: TLSv1.2")) {
                                    checkServerPassed = true;
                                } else if (!checkClientPassed
                                        && line.equals(
                                                "org.bouncycastle.jsse.provider.ProvTlsClient - Client notified of selected protocol version: TLSv1.2")) {
                                    checkClientPassed = true;
                                }
                                if (checkClientPassed && checkServerPassed) {
                                    break;
                                }
                            }
                        }
                        assertTrue(checkClientPassed, "Log file doesn't contain BouncyCastle JSSE client records");
                        assertTrue(checkServerPassed, "Log file doesn't contain BouncyCastle JSSE server records");
                    }
                });
    }
}
