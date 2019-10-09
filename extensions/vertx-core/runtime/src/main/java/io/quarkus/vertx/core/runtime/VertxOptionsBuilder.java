package io.quarkus.vertx.core.runtime;

import static io.vertx.core.file.impl.FileResolver.CACHE_DIR_BASE_PROP_NAME;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.vertx.core.runtime.config.ClusterConfiguration;
import io.quarkus.vertx.core.runtime.config.EventBusConfiguration;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;

public class VertxOptionsBuilder {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    static VertxOptions buildVertxOptions(VertxConfiguration conf) {
        VertxOptions options = new VertxOptions();

        // Order matters, as the cluster options modifies the event bus options.
        buildEventBusOptions(conf, options);
        buildClusterOptions(conf, options);
        buildFileSystemOptions(conf, options);

        options.setWorkerPoolSize(conf.workerPoolSize);
        options.setInternalBlockingPoolSize(conf.internalBlockingPoolSize);

        options.setBlockedThreadCheckInterval(conf.warningExceptionTime.toMillis());
        options.setWarningExceptionTime(conf.warningExceptionTime.toNanos());
        conf.maxEventLoopExecuteTime.ifPresent(d -> options.setMaxEventLoopExecuteTime(d.toMillis()));
        conf.maxWorkerExecuteTime.ifPresent(d -> options.setMaxWorkerExecuteTime(d.toMillis()));

        conf.eventLoopsPoolSize.ifPresent(options::setEventLoopPoolSize);

        return options;
    }

    private static void buildFileSystemOptions(VertxConfiguration conf, VertxOptions options) {
        String fileCacheDir = System.getProperty(CACHE_DIR_BASE_PROP_NAME,
                System.getProperty("java.io.tmpdir", ".") + File.separator + "quarkus-vertx-cache");

        options.setFileSystemOptions(new FileSystemOptions()
                .setFileCacheDir(fileCacheDir)
                .setFileCachingEnabled(conf.caching)
                .setClassPathResolvingEnabled(conf.classpathResolving));
    }

    private static void buildClusterOptions(VertxConfiguration conf, VertxOptions options) {
        ClusterConfiguration cluster = conf.cluster;
        EventBusOptions eb = options.getEventBusOptions();
        eb.setClustered(cluster.clustered);
        eb.setClusterPingReplyInterval(cluster.pingReplyInterval.toMillis());
        eb.setClusterPingInterval(cluster.pingInterval.toMillis());
        if (cluster.host != null) {
            eb.setHost(cluster.host);
        }
        cluster.port.ifPresent(eb::setPort);
        cluster.publicHost.ifPresent(eb::setClusterPublicHost);
        cluster.publicPort.ifPresent(eb::setClusterPublicPort);
    }

    private static void buildEventBusOptions(VertxConfiguration conf, VertxOptions options) {
        EventBusConfiguration eb = conf.eventbus;
        EventBusOptions opts = new EventBusOptions();
        opts.setAcceptBacklog(eb.acceptBacklog.orElse(-1));
        opts.setClientAuth(ClientAuth.valueOf(eb.clientAuth.toUpperCase()));
        opts.setConnectTimeout((int) (Math.min(Integer.MAX_VALUE, eb.connectTimeout.toMillis())));
        opts.setIdleTimeout(
                eb.idleTimeout.map(d -> Math.max(1, (int) Math.min(Integer.MAX_VALUE, d.getSeconds()))).orElse(0));
        opts.setSendBufferSize(eb.sendBufferSize.orElse(-1));
        opts.setSoLinger(eb.soLinger.orElse(-1));
        opts.setSsl(eb.ssl);
        opts.setReceiveBufferSize(eb.receiveBufferSize.orElse(-1));
        opts.setReconnectAttempts(eb.reconnectAttempts);
        opts.setReconnectInterval(eb.reconnectInterval.toMillis());
        opts.setReuseAddress(eb.reuseAddress);
        opts.setReusePort(eb.reusePort);
        opts.setTrafficClass(eb.trafficClass.orElse(-1));
        opts.setTcpKeepAlive(eb.tcpKeepAlive);
        opts.setTcpNoDelay(eb.tcpNoDelay);
        opts.setTrustAll(eb.trustAll);

        // Certificates and trust.
        if (eb.keyCertificatePem != null) {
            List<String> certs = new ArrayList<>();
            List<String> keys = new ArrayList<>();
            eb.keyCertificatePem.certs.ifPresent(
                    s -> certs.addAll(COMMA_PATTERN.splitAsStream(s).map(String::trim).collect(Collectors.toList())));
            eb.keyCertificatePem.keys.ifPresent(
                    s -> keys.addAll(COMMA_PATTERN.splitAsStream(s).map(String::trim).collect(Collectors.toList())));
            PemKeyCertOptions o = new PemKeyCertOptions()
                    .setCertPaths(certs)
                    .setKeyPaths(keys);
            opts.setPemKeyCertOptions(o);
        }

        if (eb.keyCertificateJks != null) {
            JksOptions o = new JksOptions();
            eb.keyCertificateJks.path.ifPresent(o::setPath);
            eb.keyCertificateJks.password.ifPresent(o::setPassword);
            opts.setKeyStoreOptions(o);
        }

        if (eb.keyCertificatePfx != null) {
            PfxOptions o = new PfxOptions();
            eb.keyCertificatePfx.path.ifPresent(o::setPath);
            eb.keyCertificatePfx.password.ifPresent(o::setPassword);
            opts.setPfxKeyCertOptions(o);
        }

        if (eb.trustCertificatePem != null) {
            eb.trustCertificatePem.certs.ifPresent(s -> {
                PemTrustOptions o = new PemTrustOptions();
                COMMA_PATTERN.splitAsStream(s).map(String::trim).forEach(o::addCertPath);
                opts.setPemTrustOptions(o);
            });
        }

        if (eb.trustCertificateJks != null) {
            JksOptions o = new JksOptions();
            eb.trustCertificateJks.path.ifPresent(o::setPath);
            eb.trustCertificateJks.password.ifPresent(o::setPassword);
            opts.setTrustStoreOptions(o);
        }

        if (eb.trustCertificatePfx != null) {
            PfxOptions o = new PfxOptions();
            eb.trustCertificatePfx.path.ifPresent(o::setPath);
            eb.trustCertificatePfx.password.ifPresent(o::setPassword);
            opts.setPfxTrustOptions(o);
        }
        options.setEventBusOptions(opts);
    }
}
