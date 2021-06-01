package io.quarkus.vertx.core.runtime;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder.VertxOptionsCustomizer;
import io.quarkus.vertx.core.runtime.config.AddressResolverConfiguration;
import io.quarkus.vertx.core.runtime.config.ClusterConfiguration;
import io.quarkus.vertx.core.runtime.config.EventBusConfiguration;
import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;

public class VertxCoreProducerTest {

    private VertxCoreRecorder recorder;

    @BeforeEach
    public void setUp() throws Exception {
        recorder = new VertxCoreRecorder();
    }

    @AfterEach
    public void tearDown() throws Exception {
        recorder.destroy();
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
            VertxCoreRecorder.initialize(configuration, null, null, LaunchMode.TEST);
            Assertions.fail("It should not have a cluster manager on the classpath, and so fail the creation");
        } catch (IllegalStateException e) {
            Assertions.assertTrue(e.getMessage().contains("No ClusterManagerFactory"),
                    "The message should contain ''. Message: " + e.getMessage());
        }
    }

    @Test
    public void shouldConfigureAddressResolver() {
        VertxConfiguration configuration = createDefaultConfiguration();
        AddressResolverConfiguration ar = configuration.resolver;
        ar.cacheMaxTimeToLive = 3;
        ar.cacheNegativeTimeToLive = 1;

        VertxOptionsCustomizer customizers = new VertxOptionsCustomizer(Arrays.asList(
                new Consumer<VertxOptions>() {
                    @Override
                    public void accept(VertxOptions vertxOptions) {
                        Assertions.assertEquals(3, vertxOptions.getAddressResolverOptions().getCacheMaxTimeToLive());
                        Assertions.assertEquals(
                                AddressResolverOptions.DEFAULT_CACHE_MIN_TIME_TO_LIVE,
                                vertxOptions.getAddressResolverOptions().getCacheMinTimeToLive());
                        Assertions.assertEquals(1, vertxOptions.getAddressResolverOptions().getCacheNegativeTimeToLive());
                    }
                }));

        VertxCoreRecorder.initialize(configuration, customizers, null, LaunchMode.TEST);
    }

    @Test
    public void shouldInvokeCustomizers() {
        final AtomicBoolean called = new AtomicBoolean(false);
        VertxOptionsCustomizer customizers = new VertxOptionsCustomizer(Arrays.asList(
                new Consumer<VertxOptions>() {
                    @Override
                    public void accept(VertxOptions vertxOptions) {
                        called.set(true);
                    }
                }));
        Vertx v = VertxCoreRecorder.initialize(createDefaultConfiguration(), customizers, null, LaunchMode.TEST);
        Assertions.assertTrue(called.get(), "Customizer should get called during initialization");
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
        vc.queueSize = OptionalInt.empty();
        vc.keepAliveTime = Duration.ofSeconds(30);
        vc.useAsyncDNS = false;
        vc.preferNativeTransport = false;
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
        vc.resolver = new AddressResolverConfiguration();
        vc.resolver.cacheMaxTimeToLive = Integer.MAX_VALUE;
        vc.resolver.cacheMinTimeToLive = 0;
        vc.resolver.cacheNegativeTimeToLive = 0;
        return vc;
    }
}
