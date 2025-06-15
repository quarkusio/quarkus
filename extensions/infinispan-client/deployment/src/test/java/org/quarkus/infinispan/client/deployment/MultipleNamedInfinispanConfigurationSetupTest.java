package org.quarkus.infinispan.client.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.infinispan.client.InfinispanClientName;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleNamedInfinispanConfigurationSetupTest {
    @Inject
    RemoteCacheManager remoteCacheManager;

    @Inject
    @InfinispanClientName("another")
    RemoteCacheManager anotherRemoteCacheManager;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withEmptyApplication()
            .withConfigurationResource("multiple-cache-config-application.properties").withApplicationRoot(
                    (jar) -> jar.addAsResource("distributed-cache-config.xml").addAsResource("local-cache-config.xml"));

    @Test
    public void infinispanMultipleConnectionsConfiguration() {
        assertThat(remoteCacheManager).isNotNull();
        assertThat(anotherRemoteCacheManager).isNotNull();

        Configuration configuration = remoteCacheManager.getConfiguration();
        assertThat(configuration.servers().size()).isEqualTo(1);
        assertThat(configuration.servers().get(0).host()).isEqualTo("cluster1");
        assertThat(configuration.servers().get(0).port()).isEqualTo(31000);
        assertThat(configuration.remoteCaches().get("cache3").configuration()).isEqualTo("<local-cache/>");

        Configuration anotherConfiguration = anotherRemoteCacheManager.getConfiguration();
        assertThat(anotherConfiguration.servers().size()).isEqualTo(1);
        assertThat(anotherConfiguration.servers().get(0).host()).isEqualTo("cluster2");
        assertThat(anotherConfiguration.servers().get(0).port()).isEqualTo(41000);
        assertThat(anotherConfiguration.remoteCaches().get("cache4").configuration()).isEqualTo("<local-cache/>");
    }
}
