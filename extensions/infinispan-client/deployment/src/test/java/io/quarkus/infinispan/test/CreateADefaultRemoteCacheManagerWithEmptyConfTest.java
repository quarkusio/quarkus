
package io.quarkus.infinispan.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Default;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.counter.api.CounterManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class CreateADefaultRemoteCacheManagerWithEmptyConfTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("empty-application-infinispan-client.properties");

    @Test
    public void remoteCacheManagerDefaultBeansAccessible() {
        assertThat(Arc.container().instance(RemoteCacheManager.class, Default.Literal.INSTANCE).get()).isNotNull();
        assertThat(Arc.container().instance(CounterManager.class, Default.Literal.INSTANCE).get()).isNotNull();
        assertThat(Arc.container().listAll(RemoteCacheManager.class).size()).isEqualTo(1);
        assertThat(Arc.container().listAll(CounterManager.class).size()).isEqualTo(1);
    }
}
