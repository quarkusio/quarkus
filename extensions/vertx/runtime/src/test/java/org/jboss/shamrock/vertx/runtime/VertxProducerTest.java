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
        producer.clustered = false;
        producer.workerPoolSize = 20;
        producer.internalBlockingPoolSize = 20;
        producer.warningExceptionTime = 2;
        producer.eventLoopsPoolSize = Optional.empty();
        producer.maxEventLoopExecuteTime = 2;
        producer.maxWorkerExecuteTime = 60;

        assertThat(producer.vertx(), is(notNullValue()));
        assertFalse(producer.vertx().isClustered());
        assertThat(producer.eventbus(), is(notNullValue()));

        producer.destroy();
    }

    @Test
    public void shouldNotFailWithDefaultConfig() {
        VertxProducer producer = new VertxProducer();
        producer.clustered = false;
        producer.workerPoolSize = 5;
        producer.internalBlockingPoolSize = 20;
        producer.warningExceptionTime = 2;
        producer.eventLoopsPoolSize = Optional.empty();
        producer.maxEventLoopExecuteTime = 1;
        producer.maxWorkerExecuteTime = 10;

        assertThat(producer.vertx(), is(notNullValue()));
        assertFalse(producer.vertx().isClustered());
        assertThat(producer.eventbus(), is(notNullValue()));

        producer.destroy();
    }

    @Test
    public void shouldEnableClustering() {
        VertxProducer producer = new VertxProducer();
        producer.clustered = true;
        producer.pingReplyInterval = 2;
        producer.pingInterval = 2;
        producer.clusterPublicHost = Optional.empty();
        producer.clusterPublicPort = Optional.empty();
        producer.clusterHost = "localhost";
        producer.clusterPort = Optional.empty();
        producer.workerPoolSize = 5;
        producer.internalBlockingPoolSize = 20;
        producer.warningExceptionTime = 2;
        producer.eventLoopsPoolSize = Optional.empty();
        producer.maxEventLoopExecuteTime = 1;
        producer.maxWorkerExecuteTime = 10;

        producer.workerPoolSize = 10;
        producer.warningExceptionTime = 1;
        producer.internalBlockingPoolSize = 5;
        producer.connectTimeout = Optional.of(60L);
        producer.clientAuth = Optional.empty();
        producer.idleTimeout = Optional.empty();
        producer.ssl = false;
        producer.reconnectAttempts = Optional.empty();
        producer.reconnectInterval = Optional.empty();
        producer.reuseAddress = Optional.empty();
        producer.reusePort = Optional.empty();
        producer.keyStorePath = Optional.empty();
        producer.trustStorePath = Optional.empty();


        try {
            producer.vertx();
            fail("It should not have a cluster manager on the classpath, and so fail the creation");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No ClusterManagerFactory"));
        }

        producer.destroy();
    }
}