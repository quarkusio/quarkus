package io.quarkus.vertx.core.runtime;

import static io.vertx.core.file.impl.FileResolver.CACHE_DIR_BASE_PROP_NAME;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.wildfly.common.cpu.ProcessorInfo;

import io.netty.channel.EventLoopGroup;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.IOThreadDetector;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.core.runtime.config.ClusterConfiguration;
import io.quarkus.vertx.core.runtime.config.EventBusConfiguration;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
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
import io.vertx.core.spi.resolver.ResolverProvider;

@Recorder
public class VertxCoreRecorder {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    static volatile VertxSupplier vertx;
    //temporary vertx instance to work around a JAX-RS problem
    static volatile Vertx webVertx;

    public Supplier<Vertx> configureVertx(BeanContainer container, VertxConfiguration config,
            LaunchMode launchMode, ShutdownContext shutdown) {
        vertx = new VertxSupplier(config);
        VertxCoreProducer producer = container.instance(VertxCoreProducer.class);
        producer.initialize(vertx);
        if (launchMode != LaunchMode.DEVELOPMENT) {
            // we need this to be part of the last shutdown tasks because closing it early (basically before Arc)
            // could cause problem to beans that rely on Vert.x and contain shutdown tasks
            shutdown.addLastShutdownTask(new Runnable() {
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
            VertxOptions options = convertToVertxOptions(conf, false);
            webVertx = Vertx.vertx(options);
        }
    }

    public static Vertx initialize(VertxConfiguration conf) {
        if (conf == null) {
            return Vertx.vertx();
        }

        VertxOptions options = convertToVertxOptions(conf, true);

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

    private static VertxOptions convertToVertxOptions(VertxConfiguration conf, boolean allowClustering) {
        if (!conf.useAsyncDNS) {
            System.setProperty(ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME, "true");
        }

        VertxOptions options = new VertxOptions();

        if (allowClustering) {
            // Order matters, as the cluster options modifies the event bus options.
            setEventBusOptions(conf, options);
            initializeClusterOptions(conf, options);
        }

        String fileCacheDir = System.getProperty(CACHE_DIR_BASE_PROP_NAME,
                System.getProperty("java.io.tmpdir", ".") + File.separator + "vertx-cache");

        options.setFileSystemOptions(new FileSystemOptions()
                .setFileCachingEnabled(conf.caching)
                .setFileCacheDir(fileCacheDir)
                .setClassPathResolvingEnabled(conf.classpathResolving));
        options.setWorkerPoolSize(conf.workerPoolSize);
        options.setInternalBlockingPoolSize(conf.internalBlockingPoolSize);

        options.setBlockedThreadCheckInterval(conf.warningExceptionTime.toMillis());
        if (conf.eventLoopsPoolSize.isPresent()) {
            options.setEventLoopPoolSize(conf.eventLoopsPoolSize.getAsInt());
        } else {
            options.setEventLoopPoolSize(calculateDefaultIOThreads());
        }

        Optional<Duration> maxEventLoopExecuteTime = conf.maxEventLoopExecuteTime;
        if (maxEventLoopExecuteTime.isPresent()) {
            options.setMaxEventLoopExecuteTime(maxEventLoopExecuteTime.get().toMillis());
            options.setMaxEventLoopExecuteTimeUnit(TimeUnit.MILLISECONDS);
        }

        Optional<Duration> maxWorkerExecuteTime = conf.maxWorkerExecuteTime;
        if (maxWorkerExecuteTime.isPresent()) {
            options.setMaxWorkerExecuteTime(maxWorkerExecuteTime.get().toMillis());
            options.setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS);
        }

        options.setWarningExceptionTime(conf.warningExceptionTime.toNanos());

        return options;
    }

    private static int calculateDefaultIOThreads() {
        //we only allow one event loop per 10mb of ram at the most
        //its hard to say what this number should be, but it is also obvious
        //that for constrained environments we don't want a lot of event loops
        //lets start with 10mb and adjust as needed
        int recommended = ProcessorInfo.availableProcessors() * 2;
        long mem = Runtime.getRuntime().maxMemory();
        long memInMb = mem / (1024 * 1024);
        long maxAllowed = memInMb / 10;

        return (int) Math.max(2, Math.min(maxAllowed, recommended));
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

    public Supplier<Integer> calculateEventLoopThreads(VertxConfiguration conf) {
        int threads;
        if (conf.eventLoopsPoolSize.isPresent()) {
            threads = conf.eventLoopsPoolSize.getAsInt();
        } else {
            threads = calculateDefaultIOThreads();
        }
        return new Supplier<Integer>() {
            @Override
            public Integer get() {
                return threads;
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
