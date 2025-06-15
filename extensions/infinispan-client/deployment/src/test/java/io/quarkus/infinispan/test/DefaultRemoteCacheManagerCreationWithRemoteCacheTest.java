
package io.quarkus.infinispan.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.counter.api.CounterManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.infinispan.client.Remote;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultRemoteCacheManagerCreationWithRemoteCacheTest {

    // tag::injection[]
    @Inject
    @Remote("cache1") // default connection
    RemoteCache<String, String> cache1;

    @Inject
    @Remote("cache2") // default connection
    RemoteCache cache2;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("empty-application-infinispan-client.properties");

    @Test
    public void remoteCacheManagerDefaultBeansAccessible() {
        assertThat(Arc.container().instance(RemoteCacheManager.class, Default.Literal.INSTANCE).get()).isNotNull();
        assertThat(Arc.container().instance(CounterManager.class, Default.Literal.INSTANCE).get()).isNotNull();
        assertThat(Arc.container().listAll(RemoteCacheManager.class).size()).isEqualTo(1);
        assertThat(Arc.container().listAll(CounterManager.class).size()).isEqualTo(1);
        assertThat(Arc.container().listAll(RemoteCache.class).size()).isEqualTo(2);
    }

    @Test
    public void cachesAreAccessible() {
        assertThat(cache1).isNotNull();
        assertThat(cache2).isNotNull();
    }
}
