package io.quarkus.vertx.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.vertx.core.runtime.config.ClusterConfiguration;
import io.quarkus.vertx.core.runtime.config.EventBusConfiguration;
import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.Vertx;

public class VertxCoreProducerTest {

    private VertxCoreRecorder recorder;
    private VertxCoreProducer producer;

    @BeforeEach
    public void setUp() throws Exception {
        producer = new VertxCoreProducer();
        recorder = new VertxCoreRecorder();
    }

    @AfterEach
    public void tearDown() throws Exception {
        recorder.destroy();
    }

    @Test
    public void shouldNotFailWithoutConfig() {
        producer.initialize(new Supplier<Vertx>() {
            @Override
            public Vertx get() {
                return VertxCoreRecorder.initialize(null);
            }
        });
        verifyProducer();
    }

    private void verifyProducer() {
        assertThat(producer.vertx()).isNotNull();
        assertFalse(producer.vertx().isClustered());
    }

    @Test
    public void shouldNotFailWithDefaultConfig() {
        VertxConfiguration configuration = createDefaultConfiguration();
        configuration.workerPoolSize = 10;
        configuration.warningExceptionTime = Duration.ofSeconds(1);
        configuration.internalBlockingPoolSize = 5;
        VertxCoreRecorder.vertx = new VertxCoreRecorder.VertxSupplier(configuration);
        producer.initialize(VertxCoreRecorder.vertx);
        verifyProducer();
    }

    @Test
    public void shouldEnableClustering() {
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

        try {
            VertxCoreRecorder.initialize(configuration);
            fail("It should not have a cluster manager on the classpath, and so fail the creation");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No ClusterManagerFactory"));
        }
    }

    private VertxConfiguration createDefaultConfiguration() {
        final VertxConfiguration vc = new VertxConfiguration();
        vc.caching = true;
        vc.classpathResolving = true;
        vc.eventLoopsPoolSize = OptionalInt.empty();
        vc.maxEventLoopExecuteTime = Optional.of(Duration.ofSeconds(2));
        vc.warningExceptionTime = Duration.ofSeconds(2);
        vc.workerPoolSize = 20;
        vc.maxWorkerExecuteTime = Optional.of(Duration.ofSeconds(1));
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
