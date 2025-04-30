package org.quarkus.infinispan.client.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InfinispanConfigurationSetupTest {
    @Inject
    RemoteCacheManager remoteCacheManager;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("cache-config-application.properties")
            .withApplicationRoot((jar) -> jar
                    .addAsResource("distributed-cache-config.xml")
                    .addAsResource("local-cache-config.xml"));

    @Test
    public void infinispanConnectionConfiguration() {
        assertThat(remoteCacheManager).isNotNull();
        Configuration configuration = remoteCacheManager.getConfiguration();
        assertThat(configuration.servers()).extracting("host", "port").containsExactly(tuple("cluster1", 31000));
        assertThat(configuration.tracingPropagationEnabled()).isFalse();
        assertThat(configuration.clientIntelligence()).isEqualTo(ClientIntelligence.BASIC);
        assertThat(configuration.security().authentication().enabled()).isTrue();
        assertThat(configuration.security().authentication().saslMechanism()).isEqualTo("BASIC");
        assertThat(configuration.security().authentication().serverName()).isEqualTo("custom-server-name");
        assertThat(configuration.security().ssl().enabled()).isTrue();
        assertThat(configuration.security().ssl().trustStorePassword()).isEqualTo("trust-pass".toCharArray());
        assertThat(configuration.security().ssl().trustStoreFileName()).isEqualTo("trustFileName.pfx");
        assertThat(configuration.security().ssl().trustStoreType()).isEqualTo("JCEKS");
        assertThat(configuration.security().ssl().keyStoreFileName()).isEqualTo("keyStoreFile.pfx");
        assertThat(configuration.security().ssl().keyStoreType()).isEqualTo("PKCS12");
        assertThat(configuration.security().ssl().keyStorePassword()).isEqualTo("key-pass".toCharArray());
        assertThat(configuration.security().ssl().keyAlias()).isEqualTo("keyAlias");
        assertThat(configuration.security().ssl().provider()).isEqualTo("SSL_prov");
        assertThat(configuration.security().ssl().protocol()).isEqualTo("SSL_protocol");
        assertThat(configuration.security().ssl().ciphers()).containsExactlyInAnyOrder("SSL_cipher1", "SSL_cipher2");
        assertThat(configuration.security().ssl().hostnameValidation()).isTrue();
        assertThat(configuration.security().ssl().sniHostName()).isEqualTo("sniHostName");
        assertThat(configuration.socketTimeout()).isEqualTo(10000);
        assertThat(configuration.clusters()).extracting("clusterName", "clientIntelligence")
                .containsExactly(tuple("bsite", ClientIntelligence.BASIC));
        assertThat(configuration.clusters()).hasSize(1);
        assertThat(configuration.clusters().get(0).getCluster()).extracting("host", "port")
                .containsExactly(tuple("bsite1", 32111));

        assertThat(configuration.remoteCaches().get("cache1").configuration()).isEqualTo("<replicated-cache/>");
        assertThat(configuration.remoteCaches().get("cache1").nearCacheBloomFilter()).isTrue();
        assertThat(configuration.remoteCaches().get("cache1").nearCacheMaxEntries()).isEqualTo(100);
        assertThat(configuration.remoteCaches().get("cache1").nearCacheMode()).isEqualTo(NearCacheMode.INVALIDATED);

        assertThat(configuration.remoteCaches().get("cache2").configuration()).isEqualTo("<distributed-cache/>");
        assertThat(configuration.remoteCaches().get("cache2").nearCacheBloomFilter()).isFalse();
        assertThat(configuration.remoteCaches().get("cache2").nearCacheMaxEntries()).isEqualTo(-1);
        assertThat(configuration.remoteCaches().get("cache2").nearCacheMode()).isEqualTo(NearCacheMode.DISABLED);

        assertThat(configuration.remoteCaches().get("cache3").configuration()).isEqualTo("<local-cache/>");

    }
}
