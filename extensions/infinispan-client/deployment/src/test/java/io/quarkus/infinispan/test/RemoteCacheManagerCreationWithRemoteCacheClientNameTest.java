
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

public class RemoteCacheManagerCreationWithRemoteCacheClientNameTest {

    // tag::injection[]
    @Inject
    @InfinispanClientName("conn-2")
    @Remote("cache1")
    RemoteCache<String, String> cache1;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("dev-service-conn-2-application-infinispan-client.properties");

    @Test
    public void remoteCacheManagerDefaultBeansAccessible() {
        assertThat(Arc.container().instance(RemoteCacheManager.class, Default.Literal.INSTANCE).get()).isNull();
        assertThat(Arc.container().instance(CounterManager.class, Default.Literal.INSTANCE).get()).isNull();
        assertThat(Arc.container().instance(RemoteCacheManager.class, NamedLiteral.of("conn-2")).get()).isNotNull();
        assertThat(Arc.container().instance(CounterManager.class, NamedLiteral.of("conn-2")).get()).isNotNull();
        assertThat(Arc.container().listAll(RemoteCache.class).size()).isEqualTo(1);
    }

    @Test
    public void cacheIsAccessible() {
        assertThat(cache1).isNotNull();
    }
}
