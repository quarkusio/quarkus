package org.jboss.shamrock.vertx.runtime;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

public class VertxProducerTest {


    @Test
    public void shouldNotFailWithoutConfig() {
        VertxProducer producer = new VertxProducer();
        producer.configure(null);

        assertThat(producer.vertx(), is(notNullValue()));
        assertFalse(producer.vertx().isClustered());
        assertThat(producer.eventbus(), is(notNullValue()));

        producer.destroy();
    }

    @Test
    public void shouldNotFailWithDefaultConfig() {
        VertxProducer producer = new VertxProducer();
        VertxConfiguration configuration = new VertxConfiguration();
        configuration.eventBusConfiguration = Optional.empty();
        configuration.clusterConfiguration = Optional.empty();
        configuration.workerPoolSize = 10;
        configuration.warningExceptionTime = 1000;
        configuration.internalBlockingPoolSize = 5;
        producer.configure(configuration);

        assertThat(producer.vertx(), is(notNullValue()));
        assertFalse(producer.vertx().isClustered());
        assertThat(producer.eventbus(), is(notNullValue()));

        producer.destroy();
    }

    @Test
    public void shouldEnableClustering() {
        VertxProducer producer = new VertxProducer();
        VertxConfiguration configuration = new VertxConfiguration();
        ClusterConfiguration cc = new ClusterConfiguration();
        cc.clustered = true;
        cc.pingReplyInterval = 2000;
        cc.pingInterval = 2000;
        cc.publicHost = Optional.empty();
        configuration.workerPoolSize = 10;
        configuration.warningExceptionTime = 1000;
        configuration.internalBlockingPoolSize = 5;

        configuration.clusterConfiguration = Optional.of(cc);
        configuration.eventBusConfiguration = Optional.empty();

        producer.configure(configuration);
        try {
            producer.vertx();
            fail("It should not have a cluster manager on the classpath, and so fail the creation");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No ClusterManagerFactory"));
        }

        producer.destroy();
    }

}