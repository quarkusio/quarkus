package io.quarkus.reactive.db2.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.CreationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that when {@code tls-configuration-name} is set but {@code ssl} is NOT explicitly set,
 * DB2 auto-enables SSL. The startup fails because the named TLS config doesn't exist,
 * but reaching that error proves the TLS code path was entered.
 */
public class TlsRegistryAutoSslModeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "db2")
            .overrideConfigKey("quarkus.datasource.reactive.url", "db2://localhost:50000/hreact")
            .overrideConfigKey("quarkus.datasource.reactive.tls-configuration-name", "auto-ssl-test")
            .assertException(t -> {
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
