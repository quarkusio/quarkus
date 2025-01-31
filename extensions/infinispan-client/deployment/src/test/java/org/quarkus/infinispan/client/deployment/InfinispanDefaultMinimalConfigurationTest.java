package org.quarkus.infinispan.client.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InfinispanDefaultMinimalConfigurationTest {
    @Inject
    RemoteCacheManager remoteCacheManager;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("minimal-application.properties");

    @Test
    public void infinispanConnectionConfiguration() {
        assertThat(remoteCacheManager).isNotNull();
        Configuration configuration = remoteCacheManager.getConfiguration();
        assertThat(configuration.servers().size()).isEqualTo(1);
        assertThat(configuration.servers().get(0).host()).isEqualTo("cluster1");
        assertThat(configuration.servers().get(0).port()).isEqualTo(31000);
        assertThat(configuration.tracingPropagationEnabled()).isTrue();
        assertThat(configuration.clientIntelligence()).isEqualTo(ClientIntelligence.HASH_DISTRIBUTION_AWARE);
        assertThat(configuration.remoteCaches()).isEmpty();
        assertThat(configuration.security().authentication().enabled()).isTrue();
        assertThat(configuration.security().authentication().saslMechanism()).isEqualTo("DIGEST-SHA-512");
        assertThat(configuration.security().ssl().enabled()).isFalse();
    }
}
