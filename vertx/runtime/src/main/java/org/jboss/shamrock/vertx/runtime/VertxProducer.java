package org.jboss.shamrock.vertx.runtime;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Produces a configured Vert.x instance.
 * It also exposes the Vert.x event bus.
 */
@ApplicationScoped
public class VertxProducer {


    private VertxConfiguration conf;
    private Vertx vertx;

    private void initialize() {
        if (conf == null) {
            this.vertx = Vertx.vertx();
            return;
        }

        VertxOptions options = convertToVertxOptions(conf);

        if (!conf.useAsyncDNS) {
            System.setProperty("vertx.disableDnsResolver", "true");
        }

        System.setProperty("vertx.cacheDirBase", System.getProperty("java.io.tmpdir"));

        if (options.isClustered()) {
            AtomicReference<Throwable> failure = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            Vertx.clusteredVertx(options, ar -> {
                if (ar.failed()) {
                    failure.set(ar.cause());
                } else {
                    this.vertx = ar.result();
                }
                latch.countDown();
            });
            try {
                latch.await();
                if (failure.get() != null) {
                    throw new IllegalStateException("Unable to initialize the Vert.x instance", failure.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Unable to initialize the Vert.x instance", e);
            }
        } else {
            this.vertx = Vertx.vertx(options);
        }
    }

    private VertxOptions convertToVertxOptions(VertxConfiguration conf) {
        VertxOptions options = new VertxOptions();
        // Order matters, as the cluster options modifies the event bus options.
        setEventBusOptions(options);
        initializeClusterOptions(options);

        options.setFileResolverCachingEnabled(conf.fileResolverCachingEnabled);
        options.setWorkerPoolSize(conf.workerPoolSize);
        options.setBlockedThreadCheckInterval(conf.warningExceptionTime);
        options.setInternalBlockingPoolSize(conf.internalBlockingPoolSize);
        if (conf.eventLoopsPoolSize > 0) {
            options.setEventLoopPoolSize(conf.eventLoopsPoolSize);
        }
        // TODO - Add the ability to configure these times in ns when long will be supported
        //  options.setMaxEventLoopExecuteTime(conf.maxEventLoopExecuteTime)
        //         .setMaxWorkerExecuteTime(conf.maxWorkerExecuteTime)
        options.setWarningExceptionTime(conf.warningExceptionTime);

        return options;
    }

    @PreDestroy
    public void destroy() {
        if (vertx != null) {
            vertx.close();
        }
    }

    private void initializeClusterOptions(VertxOptions options) {
        Optional<ClusterConfiguration> cc = conf.clusterConfiguration;
        if (!cc.isPresent()) {
            return;
        }
        ClusterConfiguration cluster = cc.get();
        options.setClustered(cluster.clustered);
        options.setClusterPingReplyInterval(cluster.pingReplyInterval);
        options.setClusterPingReplyInterval(cluster.pingInterval);
        if (cluster.host != null) {
            options.setClusterHost(cluster.host);
        }
        if (cluster.port > 0) {
            options.setClusterPort(cluster.port);
        }
        cluster.publicHost.ifPresent(options::setClusterPublicHost);
        if (cluster.publicPort > 0) {
            options.setClusterPort(cluster.publicPort);
        }
    }

    private void setEventBusOptions(VertxOptions options) {
        Optional<EventBusConfiguration> optional = conf.eventBusConfiguration;
        if (!optional.isPresent()) {
            return;
        }
        EventBusConfiguration eb = optional.get();
        EventBusOptions opts = new EventBusOptions();
        opts.setAcceptBacklog(eb.acceptBacklog);
        opts.setClientAuth(ClientAuth.valueOf(eb.clientAuth.toUpperCase()));
        opts.setConnectTimeout(eb.connectTimeout);
        opts.setIdleTimeout(eb.idleTimeout);
        opts.setSendBufferSize(eb.sendBufferSize);
        opts.setSoLinger(eb.soLinger);
        opts.setSsl(eb.ssl);
        opts.setReceiveBufferSize(eb.receiveBufferSize);
        opts.setReconnectAttempts(eb.reconnectAttempts);
        opts.setReconnectInterval(eb.reconnectInterval);
        opts.setReuseAddress(eb.reuseAddress);
        opts.setReusePort(eb.reusePort);
        opts.setTrafficClass(eb.trafficClass);
        opts.setTcpKeepAlive(eb.tcpKeepAlive);
        opts.setTcpNoDelay(eb.tcpNoDelay);
        opts.setTrustAll(eb.trustAll);

        // Certificates and trust.
        if (eb.keyPem != null) {
            List<String> certs = new ArrayList<>();
            List<String> keys = new ArrayList<>();
            eb.keyPem.certs.ifPresent(s ->
                    certs.addAll(Pattern.compile(",").splitAsStream(s).map(String::trim).collect(Collectors.toList()))
            );
            eb.keyPem.keys.ifPresent(s ->
                    keys.addAll(Pattern.compile(",").splitAsStream(s).map(String::trim).collect(Collectors.toList()))
            );
            PemKeyCertOptions o = new PemKeyCertOptions()
                    .setCertPaths(certs)
                    .setKeyPaths(keys);
            opts.setPemKeyCertOptions(o);
        }

        if (eb.keyJks != null) {
            JksOptions o = new JksOptions();
            eb.keyJks.path.ifPresent(o::setPath);
            eb.keyJks.password.ifPresent(o::setPassword);
            opts.setKeyStoreOptions(o);
        }

        if (eb.keyPfx != null) {
            PfxOptions o = new PfxOptions();
            eb.keyPfx.path.ifPresent(o::setPath);
            eb.keyPfx.password.ifPresent(o::setPassword);
            opts.setPfxKeyCertOptions(o);
        }

        if (eb.trustPem != null) {
            eb.trustPem.certs.ifPresent(s -> {
                PemTrustOptions o = new PemTrustOptions();
                Pattern.compile(",").splitAsStream(s).map(String::trim).forEach(o::addCertPath);
                opts.setPemTrustOptions(o);
            });
        }

        if (eb.trustJks != null) {
            JksOptions o = new JksOptions();
            eb.trustJks.path.ifPresent(o::setPath);
            eb.trustJks.password.ifPresent(o::setPassword);
            opts.setTrustStoreOptions(o);
        }

        if (eb.trustPfx != null) {
            PfxOptions o = new PfxOptions();
            eb.trustPfx.path.ifPresent(o::setPath);
            eb.trustPfx.password.ifPresent(o::setPassword);
            opts.setPfxTrustOptions(o);
        }
        options.setEventBusOptions(opts);
    }

    @Singleton
    @Produces
    public synchronized Vertx vertx() {
        if (vertx != null) {
            return vertx;
        }
        initialize();
        return this.vertx;
    }

    @Singleton
    @Produces
    public synchronized EventBus eventbus() {
        if (vertx == null) {
            initialize();
        }
        return this.vertx.eventBus();
    }

    void configure(VertxConfiguration config) {
        this.conf = config;
    }
}
