package io.quarkus.vertx.core.runtime;

import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;
import static io.vertx.core.file.impl.FileResolver.CACHE_DIR_BASE_PROP_NAME;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.wildfly.common.cpu.ProcessorInfo;

import io.netty.channel.EventLoopGroup;
import io.quarkus.runtime.IOThreadDetector;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.core.runtime.config.ClusterConfiguration;
import io.quarkus.vertx.core.runtime.config.EventBusConfiguration;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.spi.resolver.ResolverProvider;

@Recorder
public class VertxCoreRecorder {

    private static final Logger LOGGER = Logger.getLogger(VertxCoreRecorder.class.getName());

    static volatile VertxSupplier vertx;

    public Supplier<Vertx> configureVertx(VertxConfiguration config,
            LaunchMode launchMode, ShutdownContext shutdown, List<Consumer<VertxOptions>> customizers) {
        vertx = new VertxSupplier(config, customizers);
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

    static void shutdownDevMode() {
        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.get().close(new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> event) {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Supplier<Vertx> getVertx() {
        return vertx;
    }

    public static Vertx initialize(VertxConfiguration conf, VertxOptionsCustomizer customizer) {

        VertxOptions options = new VertxOptions();

        if (conf != null) {
            convertToVertxOptions(conf, options, true);
        }

        // Allow extension customizers to do their thing
        if (customizer != null) {
            customizer.customize(options);
        }

        Vertx vertx;
        if (options.getEventBusOptions().isClustered()) {
            CompletableFuture<Vertx> latch = new CompletableFuture<>();
            Vertx.clusteredVertx(options, ar -> {
                if (ar.failed()) {
                    latch.completeExceptionally(ar.cause());
                } else {
                    latch.complete(ar.result());
                }
            });
            vertx = latch.join();
        } else {
            vertx = Vertx.vertx(options);
        }
        return logVertxInitialization(vertx);
    }

    private static Vertx logVertxInitialization(Vertx vertx) {
        LOGGER.debugf("Vertx has Native Transport Enabled: %s", vertx.isNativeTransportEnabled());
        return vertx;
    }

    private static VertxOptions convertToVertxOptions(VertxConfiguration conf, VertxOptions options, boolean allowClustering) {

        if (!conf.useAsyncDNS) {
            System.setProperty(ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME, "true");
        }

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

        options.setPreferNativeTransport(conf.preferNativeTransport);

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
        configurePemKeyCertOptions(opts, eb.keyCertificatePem);
        configureJksKeyCertOptions(opts, eb.keyCertificateJks);
        configurePfxKeyCertOptions(opts, eb.keyCertificatePfx);

        configurePemTrustOptions(opts, eb.trustCertificatePem);
        configureJksKeyCertOptions(opts, eb.trustCertificateJks);
        configurePfxTrustOptions(opts, eb.trustCertificatePfx);

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
        final VertxOptionsCustomizer customizer;
        Vertx v;

        VertxSupplier(VertxConfiguration config, List<Consumer<VertxOptions>> customizers) {
            this.config = config;
            this.customizer = new VertxOptionsCustomizer(customizers);
        }

        @Override
        public synchronized Vertx get() {
            if (v == null) {
                v = initialize(config, customizer);
            }
            return v;
        }
    }

    static class VertxOptionsCustomizer {
        final List<Consumer<VertxOptions>> customizers;

        VertxOptionsCustomizer(List<Consumer<VertxOptions>> customizers) {
            this.customizers = customizers;
        }

        VertxOptions customize(VertxOptions options) {
            for (Consumer<VertxOptions> x : customizers) {
                x.accept(options);
            }
            return options;
        }
    }
}
