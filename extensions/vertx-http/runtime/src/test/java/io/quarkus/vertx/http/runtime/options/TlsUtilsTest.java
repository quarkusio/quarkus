package io.quarkus.vertx.http.runtime.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.CertificateConfig;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.TrustOptions;

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
            "none, pkcs11, PKCS11",
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
            "none, pem, PEM", // Failure expected
            "none, jks, JKS" // Failure expected
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

    @Test
    void testCreateKeyStoreOptionsPKCS11()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
        String type = "PKCS11";
        String password = "quarkus-pkcs11-pw";
        String alias = "quarkus-pkcs11-alias";
        Method createKeyStoreOptions = TlsUtils.class.getDeclaredMethod("createKeyStoreOptions", Path.class, Optional.class,
                String.class, Optional.class, Optional.class, Optional.class);
        createKeyStoreOptions.setAccessible(true);
        KeyStoreOptions kso = (KeyStoreOptions) createKeyStoreOptions.invoke(null, Paths.get("none"),
                Optional.of(password), type, Optional.of("SunPKCS11"), Optional.of(alias),
                Optional.empty());
        assertNotNull(kso);
        assertEquals(null, kso.getValue());
        assertEquals(type, kso.getType());
        assertEquals(alias, kso.getAlias());
    }

    @Test
    void testPKCS11KeyStoreConfigException()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
        String type = "pkcs12";
        String password = "quarkus-pkcs11-pw";
        String alias = "quarkus-pkcs11-alias";
        String pathString = "none";
        Method createKeyStoreOptions = TlsUtils.class.getDeclaredMethod("createKeyStoreOptions", Path.class, Optional.class,
                String.class, Optional.class, Optional.class, Optional.class);
        createKeyStoreOptions.setAccessible(true);

        Exception exception = assertThrows(InvocationTargetException.class, () -> {
            createKeyStoreOptions.invoke(null, Paths.get(pathString),
                    Optional.of(password), type, Optional.of("SunPKCS11"), Optional.of(alias),
                    Optional.empty());
        });

        String expectedMessage = "Keystore file property can only be set to 'none' when a keystore file type is PKCS11";
        assertEquals(ConfigurationException.class, exception.getCause().getClass());
        assertTrue(exception.getCause().getMessage().contains(expectedMessage));
    }

    @Test
    void testComputeTrustOptionsPKCS11() throws IOException {
        String type = "PKCS11";
        String password = "quarkus-pkcs11-pw";
        String alias = "quarkus-pkcs11-alias";
        String pathString = "none";
        CertificateConfig config = mock(CertificateConfig.class);

        when(config.trustStoreFile()).thenReturn(Optional.of(Paths.get(pathString)));
        when(config.trustStoreFileType()).thenReturn(Optional.of(type));
        when(config.keyStoreAlias()).thenReturn(Optional.of(alias));
        TrustOptions to = TlsUtils.computeTrustOptions(config, Optional.of(password));
        assertNotNull(to);
    }

    @Test
    void testPKCS11TrustStoreConfigException() throws IOException {
        String type = "pkcs12";
        String password = "quarkus-pkcs11-pw";
        String alias = "quarkus-pkcs11-alias";
        String pathString = "none";
        CertificateConfig config = mock(CertificateConfig.class);

        when(config.trustStoreFile()).thenReturn(Optional.of(Paths.get(pathString)));
        when(config.trustStoreFileType()).thenReturn(Optional.of(type));
        when(config.keyStoreAlias()).thenReturn(Optional.of(alias));
        Exception exception = assertThrows(ConfigurationException.class, () -> {
            TlsUtils.computeTrustOptions(config, Optional.of(password));
        });

        String expectedMessage = "Truststore file property can only be set to 'none' when a truststore file type is PKCS11";

        assertTrue(exception.getMessage().contains(expectedMessage));
    }

}
