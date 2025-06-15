package io.quarkus.infinispan.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.counter.api.CounterManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.infinispan.client.InfinispanClientName;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleNamedInfinispanClientsTest {

    // tag::injection[]
    @InfinispanClientName("site-lon")
    @Inject
    RemoteCacheManager siteLonCm;

    @InfinispanClientName("site-nyc")
    @Inject
    RemoteCacheManager siteNycCm;
    // end::injection[]

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-multiple-devservices-infinispan-client.properties");

    @Test
    public void testContainerHasBeans() {
        assertThat(Arc.container().listAll(RemoteCacheManager.class).size()).isEqualTo(2);
        assertThat(Arc.container().listAll(CounterManager.class).size()).isEqualTo(2);
        assertThat(Arc.container().instance(RemoteCacheManager.class, Default.Literal.INSTANCE).get()).isNull();
        assertThat(Arc.container().instance(CounterManager.class, Default.Literal.INSTANCE).get()).isNull();
        assertThat(Arc.container().instance(RemoteCacheManager.class, NamedLiteral.of("site-lon")).get()).isNotNull();
        assertThat(Arc.container().instance(CounterManager.class, NamedLiteral.of("site-lon")).get()).isNotNull();
        assertThat(Arc.container().instance(RemoteCacheManager.class, NamedLiteral.of("site-nyc")).get()).isNotNull();
        assertThat(Arc.container().instance(CounterManager.class, NamedLiteral.of("site-nyc")).get()).isNotNull();
    }

    @Test
    public void testNamedDevServices() {
        assertThat(siteLonCm.getConfiguration().servers().get(0).host()).isEqualTo("localhost");
        assertThat(siteLonCm.getConfiguration().servers().get(0).port()).isEqualTo(11222);
        assertThat(siteNycCm.getConfiguration().servers().get(0).host()).isEqualTo("localhost");
        assertThat(siteNycCm.getConfiguration().servers().get(0).port()).isEqualTo(31222);
    }
}
