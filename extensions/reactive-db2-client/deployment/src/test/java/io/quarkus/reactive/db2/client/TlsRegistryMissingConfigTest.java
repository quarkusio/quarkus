package io.quarkus.reactive.db2.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.CreationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class TlsRegistryMissingConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "db2")
            .overrideConfigKey("quarkus.datasource.reactive.url", "db2://localhost:50000/hreact")
            .overrideConfigKey("quarkus.datasource.reactive.tls-configuration-name", "non-existent")
            .assertException(t -> {
                assertThat(t.getClass().getName())
                        .satisfiesAnyOf(
                                name -> assertThat(name).isEqualTo(ConfigurationException.class.getName()),
                                name -> assertThat(name).isEqualTo(CreationException.class.getName()));
                assertThat(t).hasMessageContaining("non-existent");
            });

    @Test
    public void test() {
        // should not be reached — startup should fail
    }
}
