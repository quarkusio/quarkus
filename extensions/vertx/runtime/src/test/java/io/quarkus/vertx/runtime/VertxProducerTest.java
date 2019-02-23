package io.quarkus.vertx.runtime;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.Test;

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
        VertxConfiguration configuration = createDefaultConfiguration();
        configuration.workerPoolSize = 10;
        configuration.warningExceptionTime = Duration.ofSeconds(1);
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
        VertxConfiguration configuration = createDefaultConfiguration();
        ClusterConfiguration cc = configuration.cluster;
        cc.clustered = true;
        cc.pingReplyInterval = Duration.ofSeconds(2);
        cc.pingInterval = Duration.ofSeconds(2);
        cc.publicHost = Optional.empty();
        configuration.workerPoolSize = 10;
        configuration.warningExceptionTime = Duration.ofSeconds(1);
        configuration.internalBlockingPoolSize = 5;
        configuration.eventbus.connectTimeout = Duration.ofMinutes(1);
        configuration.eventbus.acceptBacklog = OptionalInt.empty();
        configuration.cluster = cc;

        producer.configure(configuration);
        try {
            producer.vertx();
            fail("It should not have a cluster manager on the classpath, and so fail the creation");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No ClusterManagerFactory"));
        }

        producer.destroy();
    }

    private VertxConfiguration createDefaultConfiguration() {
        final VertxConfiguration vc = new VertxConfiguration();
        vc.caching = true;
        vc.classpathResolving = true;
        vc.eventLoopsPoolSize = OptionalInt.empty();
        vc.maxEventLoopExecuteTime = Duration.ofSeconds(2);
        vc.warningExceptionTime = Duration.ofSeconds(2);
        vc.workerPoolSize = 20;
        vc.maxWorkerExecuteTime = Duration.ofSeconds(1);
        vc.internalBlockingPoolSize = 20;
        vc.useAsyncDNS = false;
        vc.eventbus = new EventBusConfiguration();
        vc.eventbus.keyCertificatePem = new PemKeyCertConfiguration();
        vc.eventbus.keyCertificatePem.keys = Optional.empty();
        vc.eventbus.keyCertificatePem.certs = Optional.empty();
        vc.eventbus.keyCertificateJks = new JksConfiguration();
        vc.eventbus.keyCertificateJks.path = Optional.empty();
        vc.eventbus.keyCertificateJks.password = Optional.empty();
        vc.eventbus.keyCertificatePfx = new PfxConfiguration();
        vc.eventbus.keyCertificatePfx.path = Optional.empty();
        vc.eventbus.keyCertificatePfx.password = Optional.empty();
        vc.eventbus.trustCertificatePem = new PemTrustCertConfiguration();
        vc.eventbus.trustCertificatePem.certs = Optional.empty();
        vc.eventbus.trustCertificateJks = new JksConfiguration();
        vc.eventbus.trustCertificateJks.path = Optional.empty();
        vc.eventbus.trustCertificateJks.password = Optional.empty();
        vc.eventbus.trustCertificatePfx = new PfxConfiguration();
        vc.eventbus.trustCertificatePfx.path = Optional.empty();
        vc.eventbus.trustCertificatePfx.password = Optional.empty();
        vc.eventbus.acceptBacklog = OptionalInt.empty();
        vc.eventbus.clientAuth = "NONE";
        vc.eventbus.connectTimeout = Duration.ofSeconds(60);
        vc.eventbus.idleTimeout = Optional.empty();
        vc.eventbus.receiveBufferSize = OptionalInt.empty();
        vc.eventbus.reconnectAttempts = 0;
        vc.eventbus.reconnectInterval = Duration.ofSeconds(1);
        vc.eventbus.reuseAddress = true;
        vc.eventbus.reusePort = false;
        vc.eventbus.sendBufferSize = OptionalInt.empty();
        vc.eventbus.soLinger = OptionalInt.empty();
        vc.eventbus.ssl = false;
        vc.eventbus.tcpKeepAlive = false;
        vc.eventbus.tcpNoDelay = true;
        vc.eventbus.trafficClass = OptionalInt.empty();
        vc.eventbus.trustAll = false;
        vc.cluster = new ClusterConfiguration();
        vc.cluster.host = "localhost";
        vc.cluster.port = OptionalInt.empty();
        vc.cluster.publicHost = Optional.empty();
        vc.cluster.publicPort = OptionalInt.empty();
        vc.cluster.clustered = false;
        vc.cluster.pingInterval = Duration.ofSeconds(20);
        vc.cluster.pingReplyInterval = Duration.ofSeconds(20);
        return vc;
    }
}
