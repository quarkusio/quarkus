package io.quarkus.reactive.pg.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.CreationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that when {@code tls-configuration-name} is set but {@code ssl-mode} is NOT set,
 * PG auto-defaults to {@code SslMode.REQUIRE}. The startup fails because the named TLS config
 * doesn't exist, but reaching that error proves the TLS code path was entered (which sets REQUIRE).
 */
public class TlsRegistryAutoSslModeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            .overrideConfigKey("quarkus.datasource.reactive.url", "postgresql://localhost:5431/hibernate_orm_test")
            // Set TLS config name but NOT ssl-mode — should auto-default to REQUIRE
            .overrideConfigKey("quarkus.datasource.reactive.tls-configuration-name", "auto-ssl-test")
            .assertException(t -> {
                // Check class name to avoid classloader issues
                assertThat(t.getClass().getName())
                        .satisfiesAnyOf(
                                name -> assertThat(name).isEqualTo(ConfigurationException.class.getName()),
                                name -> assertThat(name).isEqualTo(CreationException.class.getName()));
                assertThat(t).hasMessageContaining("auto-ssl-test");
            });

    @Test
    public void test() {
        // should not be reached
    }
}
