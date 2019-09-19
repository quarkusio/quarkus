package io.quarkus.vertx.core.runtime;

import static io.vertx.core.file.impl.FileResolver.CACHE_DIR_BASE_PROP_NAME;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.netty.channel.EventLoopGroup;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.IOThreadDetector;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;

@Recorder
public class VertxCoreRecorder {

    public static final String ENABLE_JSON = "quarkus-internal.vertx.enabled-json";

    static volatile VertxSupplier vertx;
    //temporary vertx instance to work around a JAX-RS problem
    static volatile Vertx webVertx;

    public Supplier<Vertx> configureVertx(BeanContainer container, VertxConfiguration config,
            LaunchMode launchMode, ShutdownContext shutdown) {
        vertx = new VertxSupplier(config);
        VertxCoreProducer producer = container.instance(VertxCoreProducer.class);
        producer.initialize(vertx);
        if (launchMode != LaunchMode.DEVELOPMENT) {
            shutdown.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    destroy();
                }
            });
        }
        return vertx;
    }

    public IOThreadDetector detector() {
        return new IOThreadDetector() {
            @Override
            public boolean isInIOThread() {
                return Context.isOnEventLoopThread();
            }
        };
    }

    public static Supplier<Vertx> getVertx() {
        return vertx;
    }

    public static Vertx getWebVertx() {
        return webVertx;
    }

    public RuntimeValue<Vertx> initializeWeb(VertxConfiguration conf, ShutdownContext shutdown, LaunchMode launchMode) {
        initializeWeb(conf);
        if (launchMode != LaunchMode.DEVELOPMENT) {
            shutdown.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    destroyWeb();
                }
            });
        }
        return new RuntimeValue<>(webVertx);
    }

    public static void initializeWeb(VertxConfiguration conf) {
        if (webVertx != null) {
        } else if (conf == null) {
            webVertx = Vertx.vertx();
        } else {
            VertxOptions options = convertToVertxOptions(conf);
            webVertx = Vertx.vertx(options);
        }
    }

    public static Vertx initialize(VertxConfiguration conf) {
        if (conf == null) {
            return Vertx.vertx();
        }

        VertxOptions options = convertToVertxOptions(conf);

        if (!conf.useAsyncDNS) {
            System.setProperty("vertx.disableDnsResolver", "true");
        }

        if (options.getEventBusOptions().isClustered()) {
            CompletableFuture<Vertx> latch = new CompletableFuture<>();
            Vertx.clusteredVertx(options, ar -> {
                if (ar.failed()) {
                    latch.completeExceptionally(ar.cause());
                } else {
                    latch.complete(ar.result());
                }
            });
            return latch.join();
        } else {
            return Vertx.vertx(options);
        }
    }

    private static VertxOptions convertToVertxOptions(VertxConfiguration conf) {
        VertxOptions options = new VertxOptions();
        // Order matters, as the cluster options modifies the event bus options.
        setEventBusOptions(conf, options);
        initializeClusterOptions(conf, options);

        String fileCacheDir = System.getProperty(CACHE_DIR_BASE_PROP_NAME,
                System.getProperty("java.io.tmpdir", ".") + File.separator + "vertx-cache");

        options.setFileSystemOptions(new FileSystemOptions()
                .setFileCachingEnabled(conf.caching)
                .setFileCacheDir(fileCacheDir)
                .setClassPathResolvingEnabled(conf.classpathResolving));
        options.setWorkerPoolSize(conf.workerPoolSize);
        options.setBlockedThreadCheckInterval(conf.warningExceptionTime.toMillis());
        options.setInternalBlockingPoolSize(conf.internalBlockingPoolSize);
        if (conf.eventLoopsPoolSize.isPresent()) {
            options.setEventLoopPoolSize(conf.eventLoopsPoolSize.getAsInt());
        }
        // TODO - Add the ability to configure these times in ns when long will be supported
        //  options.setMaxEventLoopExecuteTime(conf.maxEventLoopExecuteTime)
        //         .setMaxWorkerExecuteTime(conf.maxWorkerExecuteTime)
        options.setWarningExceptionTime(conf.warningExceptionTime.toNanos());

        return options;
    }

    void destroy() {
        if (vertx != null && vertx.v != null) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> problem = new AtomicReference<>();
            vertx.v.close(ar -> {
                if (ar.failed()) {
                    problem.set(ar.cause());
                }
                latch.countDown();
            });
            try {
                latch.await();
                if (problem.get() != null) {
                    throw new IllegalStateException("Error when closing Vert.x instance", problem.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted when closing Vert.x instance", e);
            }
            vertx = null;
        }
    }

    void destroyWeb() {
        if (webVertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> problem = new AtomicReference<>();
            webVertx.close(ar -> {
                if (ar.failed()) {
                    problem.set(ar.cause());
                }
                latch.countDown();
            });
            try {
                latch.await();
                if (problem.get() != null) {
                    throw new IllegalStateException("Error when closing Vert.x instance", problem.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted when closing Vert.x instance", e);
            }
            webVertx = null;
        }
    }

    private static void initializeClusterOptions(VertxConfiguration conf, VertxOptions options) {
        ClusterConfiguration cluster = conf.cluster;
        options.getEventBusOptions().setClustered(cluster.clustered);
        options.getEventBusOptions().setClusterPingReplyInterval(cluster.pingReplyInterval.toMillis());
        options.getEventBusOptions().setClusterPingInterval(cluster.pingInterval.toMillis());
        if (cluster.host != null) {
            options.getEventBusOptions().setHost(cluster.host);
        }
        if (cluster.port.isPresent()) {
            options.getEventBusOptions().setPort(cluster.port.getAsInt());
        }
        cluster.publicHost.ifPresent(options.getEventBusOptions()::setClusterPublicHost);
        if (cluster.publicPort.isPresent()) {
            options.getEventBusOptions().setPort(cluster.publicPort.getAsInt());
        }
    }

    private static void setEventBusOptions(VertxConfiguration conf, VertxOptions options) {
        EventBusConfiguration eb = conf.eventbus;
        EventBusOptions opts = new EventBusOptions();
        opts.setAcceptBacklog(eb.acceptBacklog.orElse(-1));
        opts.setClientAuth(ClientAuth.valueOf(eb.clientAuth.toUpperCase()));
        opts.setConnectTimeout((int) (Math.min(Integer.MAX_VALUE, eb.connectTimeout.toMillis())));
        // todo: use timeUnit cleverly
        opts.setIdleTimeout(
                eb.idleTimeout.isPresent() ? (int) Math.max(1, Math.min(Integer.MAX_VALUE, eb.idleTimeout.get().getSeconds()))
                        : 0);
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
                    s -> certs.addAll(Pattern.compile(",").splitAsStream(s).map(String::trim).collect(Collectors.toList())));
            eb.keyCertificatePem.keys.ifPresent(
                    s -> keys.addAll(Pattern.compile(",").splitAsStream(s).map(String::trim).collect(Collectors.toList())));
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
                Pattern.compile(",").splitAsStream(s).map(String::trim).forEach(o::addCertPath);
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

    public Supplier<EventLoopGroup> bossSupplier() {
        return new Supplier<EventLoopGroup>() {
            @Override
            public EventLoopGroup get() {
                return ((VertxImpl) vertx.get()).getAcceptorEventLoopGroup();
            }
        };
    }

    public Supplier<EventLoopGroup> mainSupplier() {
        return new Supplier<EventLoopGroup>() {
            @Override
            public EventLoopGroup get() {
                return vertx.get().nettyEventLoopGroup();
            }
        };
    }

    static class VertxSupplier implements Supplier<Vertx> {
        final VertxConfiguration config;
        Vertx v;

        VertxSupplier(VertxConfiguration config) {
            this.config = config;
        }

        @Override
        public synchronized Vertx get() {
            if (v == null) {
                v = initialize(config);
            }
            return v;
        }
    }
}
