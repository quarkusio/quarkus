
package io.quarkus.infinispan.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.counter.api.CounterManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.infinispan.client.InfinispanClientName;
import io.quarkus.infinispan.client.Remote;
import io.quarkus.test.QuarkusUnitTest;

public class NamedAndDefaultRemoteCacheClientNameTest {

    // tag::injection[]
    @Inject
    @Remote("cache") // default connection
    RemoteCache<String, String> cache;

    @Inject
    @InfinispanClientName("conn-2") // conn-2 connection
    @Remote("cache")
    RemoteCache cacheConn2;
    // tag::injection[]

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("default-and-conn-2-application-infinispan-client.properties");

    @Test
    public void remoteCacheManagerDefaultBeansAccessible() {
        assertThat(Arc.container().instance(RemoteCacheManager.class, Default.Literal.INSTANCE).get()).isNotNull();
        assertThat(Arc.container().instance(CounterManager.class, Default.Literal.INSTANCE).get()).isNotNull();
        assertThat(Arc.container().instance(RemoteCacheManager.class, NamedLiteral.of("conn-2")).get()).isNotNull();
        assertThat(Arc.container().instance(CounterManager.class, NamedLiteral.of("conn-2")).get()).isNotNull();
        assertThat(Arc.container().listAll(RemoteCache.class).size()).isEqualTo(2);
        assertThat(Arc.container().listAll(RemoteCacheManager.class).size()).isEqualTo(2);
        assertThat(Arc.container().listAll(CounterManager.class).size()).isEqualTo(2);
    }

    @Test
    public void cachesAreAccessible() {
        assertThat(cache).isNotNull();
        assertThat(cacheConn2).isNotNull();

        assertThat(cache.getRemoteCacheContainer().getConfiguration().servers().size()).isEqualTo(1);
        assertThat(cache.getRemoteCacheContainer().getConfiguration().servers().get(0).host()).isEqualTo("localhost");
        assertThat(cache.getRemoteCacheContainer().getConfiguration().servers().get(0).port()).isEqualTo(11222);
        assertThat(cacheConn2.getRemoteCacheContainer().getConfiguration().servers().size()).isEqualTo(1);
        assertThat(cacheConn2.getRemoteCacheContainer().getConfiguration().servers().get(0).host())
                .isEqualTo("localhost");
        assertThat(cacheConn2.getRemoteCacheContainer().getConfiguration().servers().get(0).port()).isEqualTo(31222);
    }
}
