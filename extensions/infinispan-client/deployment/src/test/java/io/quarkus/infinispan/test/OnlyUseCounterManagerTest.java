package io.quarkus.infinispan.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class OnlyUseCounterManagerTest {

    // tag::injection[]
    @Inject
    CounterManager defaultCounterManager;

    // end::injection[]

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("empty-application-infinispan-client.properties");

    @Test
    public void testContainerHasBeans() {
        assertThat(Arc.container().instance(RemoteCacheManager.class, Default.Literal.INSTANCE).get()).isNotNull();
        assertThat(Arc.container().instance(CounterManager.class, Default.Literal.INSTANCE).get()).isNotNull();

        boolean created = defaultCounterManager.defineCounter("one",
                CounterConfiguration.builder(CounterType.BOUNDED_STRONG).build());
        assertThat(created).isTrue();
    }
}
