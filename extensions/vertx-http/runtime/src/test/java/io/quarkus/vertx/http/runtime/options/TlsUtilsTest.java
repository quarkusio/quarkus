package io.quarkus.vertx.http.runtime.options;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TlsUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "server-keystore.jks, JKS, JKS",
            "server-keystore.jks, jKs, JKS",
            "server-keystore.jks, null, JKS",
            "server-keystore.jks, PKCS12, PKCS12",
            "server.keystore, null, null", // Failure expected
            "server-keystore.p12, PKCS12, PKCS12",
            "server-keystore.p12, pKCs12, PKCS12",
            "server-keystore.p12, null, PKCS12",
            "server-keystore.pfx, null, PKCS12",
            "server-keystore.pkcs12, null, PKCS12",
            "server-keystore.pkcs12, JKS, JKS",
            "server.keystore.key, null, PEM",
            "server.keystore.crt, null, PEM",
            "server.keystore.pem, null, PEM",
            "server.keystore.key, JKS, JKS",
            "server.keystore.pom, PeM, PEM",
    })
    void testKeyStoreTypeDetection(String file, String userType, String expectedType) {
        Path path = new File("target/certs/" + file).toPath();
        Optional<String> type = Optional.ofNullable(userType.equals("null") ? null : userType);
        if (expectedType.equals("null")) {
            String message = assertThrows(IllegalArgumentException.class, () -> TlsUtils.getKeyStoreType(path, type))
                    .getMessage();
            assertTrue(message.contains("keystore"));
        } else {
            assertEquals(expectedType.toLowerCase(), TlsUtils.getKeyStoreType(path, type));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "server-truststore.jks, JKS, JKS",
            "server-truststore.jks, jKs, JKS",
            "server-truststore.jks, null, JKS",
            "server-truststore.jks, PKCS12, PKCS12",
            "server.truststore, null, null", // Failure expected
            "server-truststore.p12, PKCS12, PKCS12",
            "server-truststore.p12, pKCs12, PKCS12",
            "server-truststore.p12, null, PKCS12",
            "server-truststore.pfx, null, PKCS12",
            "server-truststore.pkcs12, null, PKCS12",
            "server-truststore.pkcs12, JKS, JKS",
            "server.truststore.key, null, PEM",
            "server.truststore.crt, null, PEM",
            "server.truststore.pem, null, PEM",
            "server.truststore.key, JKS, JKS",
            "server.truststore.pom, PeM, PEM",
    })
    void testTrustStoreTypeDetection(String file, String userType, String expectedType) {
        Path path = new File("target/certs/" + file).toPath();
        Optional<String> type = Optional.ofNullable(userType.equals("null") ? null : userType);
        if (expectedType.equals("null")) {
            String message = assertThrows(IllegalArgumentException.class, () -> TlsUtils.getTruststoreType(path, type))
                    .getMessage();
            assertTrue(message.contains("truststore"));
        } else {
            assertEquals(expectedType.toLowerCase(), TlsUtils.getTruststoreType(path, type));
        }
    }

}
