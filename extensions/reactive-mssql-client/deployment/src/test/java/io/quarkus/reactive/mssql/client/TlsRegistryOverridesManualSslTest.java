package io.quarkus.reactive.mssql.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.CreationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class TlsRegistryOverridesManualSslTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "mssql")
            .overrideConfigKey("quarkus.datasource.reactive.url", "sqlserver://localhost:1433/hibernate_orm_test")
            .overrideConfigKey("quarkus.datasource.reactive.tls-configuration-name", "my-tls")
            .overrideConfigKey("quarkus.datasource.reactive.trust-all", "true")
            .assertException(t -> {
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
