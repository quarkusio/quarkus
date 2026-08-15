package io.quarkus.reactive.pg.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.CreationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class TlsRegistryOverridesManualSslTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            .overrideConfigKey("quarkus.datasource.reactive.url", "postgresql://localhost:5431/hibernate_orm_test")
            .overrideConfigKey("quarkus.datasource.reactive.tls-configuration-name", "my-tls")
            // Also set manual SSL properties — these should be ignored
            .overrideConfigKey("quarkus.datasource.reactive.trust-all", "true")
            .assertException(t -> {
                // The error should be about missing TLS config, NOT about manual SSL,
                // proving the TLS registry path was taken (manual properties ignored).
                // Check class name to avoid classloader issues
                assertThat(t.getClass().getName())
                        .satisfiesAnyOf(
                                name -> assertThat(name).isEqualTo(ConfigurationException.class.getName()),
                                name -> assertThat(name).isEqualTo(CreationException.class.getName()));
                assertThat(t).hasMessageContaining("my-tls");
            });

    @Test
    public void test() {
        // should not be reached — startup should fail
    }
}
