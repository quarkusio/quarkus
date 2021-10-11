package io.quarkus.neo4j.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.util.logging.LogRecord;

import org.graalvm.nativeimage.ImageInfo;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.ssl.SslContextConfiguration;
import io.quarkus.test.InMemoryLogHandler;

class Neo4jDriverRecorderTest {

    Neo4jConfiguration emptyConfig() {
        var configShell = new Neo4jConfiguration();

        configShell.authentication = new Neo4jConfiguration.Authentication();
        configShell.authentication.username = "Thomas";
        configShell.authentication.password = "Anderson";

        configShell.pool = new Neo4jConfiguration.Pool();
        configShell.pool.maxConnectionPoolSize = 21;
        configShell.pool.connectionAcquisitionTimeout = Duration.ZERO;
        configShell.pool.idleTimeBeforeConnectionTest = Duration.ZERO;
        configShell.pool.maxConnectionLifetime = Duration.ZERO;

        return configShell;
    }

    @Test
    void shouldThrowWhenSSLIsConfiguredViaProtocolButNotAvailable() {

        runTestPretendingToBeInNativeImageWithoutSSL(() -> {
            var recorder = new Neo4jDriverRecorder();
            var config = emptyConfig();
            config.uri = "neo4j+s://somewhere";

            assertThatExceptionOfType(ConfigurationException.class)
                    .isThrownBy(() -> recorder.initializeDriver(config, new EmptyShutdownContext()))
                    .withMessage(
                            "You cannot use neo4j+s because SSL support is not available in your current native image setup.");
        });
    }

    private void runTestPretendingToBeInNativeImageWithoutSSL(Runnable test) {
        var propertyImageCodeKey = ImageInfo.PROPERTY_IMAGE_CODE_KEY;
        var oldImageCodeValue = System.getProperty(propertyImageCodeKey);

        try {
            // This makes the test pretend to run in native image mode without SSL present
            System.setProperty(propertyImageCodeKey, ImageInfo.PROPERTY_IMAGE_CODE_VALUE_RUNTIME);
            SslContextConfiguration.setSslNativeEnabled(false);

            test.run();
        } finally {
            if (oldImageCodeValue == null || oldImageCodeValue.isBlank()) {
                System.clearProperty(propertyImageCodeKey);
            } else {
                System.setProperty(propertyImageCodeKey, oldImageCodeValue);
            }
        }
    }

    @Test
    void shouldNotTouchConfigIsSecuritySchemeIsUsed() {

        var recorder = new Neo4jDriverRecorder();
        var config = emptyConfig();
        config.uri = "neo4j+s://somewhere";

        var driver = recorder.initializeDriver(config, new EmptyShutdownContext()).getValue();
        assertThat(driver.isEncrypted()).isTrue();
    }

    @Test
    void shouldEncryptWhenPossible() {

        var recorder = new Neo4jDriverRecorder();
        var config = emptyConfig();
        config.uri = "neo4j://somewhere";
        config.encrypted = true;
        config.trustSettings = new Neo4jConfiguration.TrustSettings();
        config.trustSettings.strategy = Neo4jConfiguration.TrustSettings.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES;

        var driver = recorder.initializeDriver(config, new EmptyShutdownContext()).getValue();
        assertThat(driver.isEncrypted()).isTrue();
    }

    @Test
    void shouldWarnWhenEncryptionIsNotPossible() {
        var capturingHandler = new InMemoryLogHandler(r -> r.getLoggerName().contains("Neo4jDriverRecorder"));
        InitialConfigurator.DELAYED_HANDLER.addHandler(capturingHandler);
        try {
            runTestPretendingToBeInNativeImageWithoutSSL(() -> {
                var recorder = new Neo4jDriverRecorder();
                var config = emptyConfig();
                config.uri = "neo4j://somewhere";
                config.encrypted = true;
                config.trustSettings = new Neo4jConfiguration.TrustSettings();
                config.trustSettings.strategy = Neo4jConfiguration.TrustSettings.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES;

                var driver = recorder.initializeDriver(config, new EmptyShutdownContext()).getValue();
                assertThat(driver.isEncrypted()).isFalse();

                assertThat(capturingHandler.getRecords())
                        .extracting(LogRecord::getMessage)
                        .anyMatch(m -> m.startsWith(
                                "Native SSL is disabled, communication between this client and the Neo4j server cannot be encrypted."));
            });
        } finally {
            InitialConfigurator.DELAYED_HANDLER.removeHandler(capturingHandler);
        }
    }
}
