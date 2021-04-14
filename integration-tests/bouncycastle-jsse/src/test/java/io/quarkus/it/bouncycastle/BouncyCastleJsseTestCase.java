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

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class BouncyCastleJsseTestCase {

    static final Logger LOG = Logger.getLogger(BouncyCastleJsseTestCase.class);

    @TestHTTPResource(ssl = true)
    URL url;

    @Test
    public void testListProviders() {
        doTestListProviders();
        checkLog(false);
    }

    protected void doTestListProviders() {
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
    }

    protected void checkLog(boolean serverOnly) {
        final Path logDirectory = Paths.get(".", "target");
        given().pollInterval(100, TimeUnit.MILLISECONDS)
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
                        Assertions.assertTrue(fileExists, "access log file " + accessLogFilePath + " is missing");

                        boolean checkClientPassed = serverOnly;
                        boolean checkServerPassed = false;

                        StringBuilder sbLog = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(accessLogFilePath)),
                                        StandardCharsets.UTF_8))) {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                sbLog.append(line).append("/r/n");
                                if (!checkServerPassed && line.contains("ProvTlsServer")
                                        && (line.contains("Server selected protocol version: TLSv1.2")
                                                || line.contains("Server selected protocol version: TLSv1.3"))) {
                                    checkServerPassed = true;
                                } else if (!checkClientPassed && line.contains("ProvTlsClient")
                                        && (line.contains("Client notified of selected protocol version: TLSv1.2")
                                                || line.contains("Client notified of selected protocol version: TLSv1.3"))) {
                                    checkClientPassed = true;
                                }
                                if (checkClientPassed && checkServerPassed) {
                                    break;
                                }
                            }
                        }
                        assertTrue(checkClientPassed,
                                "Log file doesn't contain BouncyCastle JSSE client records, log: " + sbLog.toString());
                        assertTrue(checkServerPassed,
                                "Log file doesn't contain BouncyCastle JSSE server records, log: " + sbLog.toString());
                    }
                });
    }
}
