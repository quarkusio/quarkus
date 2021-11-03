package io.quarkus.neo4j.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Config;

import io.quarkus.neo4j.runtime.Neo4jConfiguration.TrustSettings;

class Neo4jConfigurationTest {

    @Nested
    class TrustSettingsTest {

        @Test
        void defaultsShouldWork() {

            TrustSettings trustSettings = new TrustSettings();

            Config.TrustStrategy internalRepresentation = trustSettings.toInternalRepresentation();
            assertFalse(internalRepresentation.isHostnameVerificationEnabled());
            assertEquals(Config.TrustStrategy.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES,
                    internalRepresentation.strategy());
        }

        @Test
        void trustAllSettingShouldWork() {

            TrustSettings trustSettings = new TrustSettings();
            trustSettings.strategy = TrustSettings.Strategy.TRUST_ALL_CERTIFICATES;

            Config.TrustStrategy internalRepresentation = trustSettings.toInternalRepresentation();
            assertEquals(Config.TrustStrategy.Strategy.TRUST_ALL_CERTIFICATES, internalRepresentation.strategy());
        }

        @Test
        void hostnameVerificationSettingShouldWork() {

            TrustSettings trustSettings = new TrustSettings();
            trustSettings.hostnameVerificationEnabled = true;

            Config.TrustStrategy internalRepresentation = trustSettings.toInternalRepresentation();
            assertTrue(internalRepresentation.isHostnameVerificationEnabled());
        }

        @Test
        void customCaShouldRequireCertFile() {

            TrustSettings trustSettings = new TrustSettings();
            trustSettings.strategy = TrustSettings.Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES;

            String msg = assertThrows(RuntimeException.class, trustSettings::toInternalRepresentation)
                    .getMessage();
            assertEquals("Configured trust strategy requires a certificate file.", msg);
        }

        @Test
        void customCaShouldRequireExistingCertFile() {

            TrustSettings trustSettings = new TrustSettings();
            trustSettings.strategy = TrustSettings.Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES;
            trustSettings.certFile = Optional.of(Paths.get("na"));

            String msg = assertThrows(RuntimeException.class, trustSettings::toInternalRepresentation)
                    .getMessage();
            assertEquals("Configured trust strategy requires a certificate file.", msg);
        }

        @Test
        void trustCustomCaSettingShouldWork() throws IOException {

            TrustSettings trustSettings = new TrustSettings();
            trustSettings.strategy = TrustSettings.Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES;
            trustSettings.certFile = Optional.of(Files.createTempFile("quarkus-neo4j-test", ".cert"));

            Config.TrustStrategy internalRepresentation = trustSettings.toInternalRepresentation();
            assertEquals(Config.TrustStrategy.Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES,
                    internalRepresentation.strategy());
        }
    }
}
