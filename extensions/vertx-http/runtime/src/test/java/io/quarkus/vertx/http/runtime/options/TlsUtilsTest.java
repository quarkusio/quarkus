package io.quarkus.vertx.http.runtime.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            "server.foo, null, null", // Failure expected
            "server.truststore, null, null", // Failure expected
            "server, null, null", // Failure expected
            "server.keystore, null, JKS",
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
            "server.keystore.ca, null, null", // .ca is a truststore file
    })
    void testKeyStoreTypeDetection(String file, String userType, String expectedType) {
        Path path = new File("target/certs/" + file).toPath();
        Optional<String> type = Optional.ofNullable(userType.equals("null") ? null : userType);
        if (expectedType.equals("null")) {
            String message = assertThrows(IllegalArgumentException.class, () -> TlsUtils.getKeyStoreType(path, type))
                    .getMessage();
            assertTrue(message.contains("keystore") && message.contains("key-store-file-type"));
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
            "server.foo, null, null", // Failure expected
            "server.keystore, null, null", // Failure expected
            "server, null, null", // Failure expected
            "server.truststore, null, JKS",
            "server-truststore.p12, PKCS12, PKCS12",
            "server-truststore.p12, pKCs12, PKCS12",
            "server-truststore.p12, null, PKCS12",
            "server-truststore.pfx, null, PKCS12",
            "server-truststore.pkcs12, null, PKCS12",
            "server-truststore.pkcs12, JKS, JKS",
            "server.truststore.crt, null, PEM",
            "server.truststore.pem, null, PEM",
            "server.truststore.key, JKS, JKS",
            "server.truststore.pom, PeM, PEM",
            "server.keystore.ca, null, PEM",
            "server.keystore.key, null, null", // .key is a key file
    })
    void testTrustStoreTypeDetection(String file, String userType, String expectedType) {
        Path path = new File("target/certs/" + file).toPath();
        Optional<String> type = Optional.ofNullable(userType.equals("null") ? null : userType);
        if (expectedType.equals("null")) {
            String message = assertThrows(IllegalArgumentException.class, () -> TlsUtils.getTruststoreType(path, type))
                    .getMessage();
            assertTrue(message.contains("truststore") && message.contains("trust-store-file-type"));
        } else {
            assertEquals(expectedType.toLowerCase(), TlsUtils.getTruststoreType(path, type));
        }
    }

}
