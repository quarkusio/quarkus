package io.quarkus.vertx.core.runtime;

import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.jboss.threads.ContextHandler;
import org.jboss.threads.EnhancedQueueExecutor;
import org.wildfly.common.cpu.ProcessorInfo;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FastThreadLocal;
import io.quarkus.runtime.IOThreadDetector;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.core.runtime.config.AddressResolverConfiguration;
import io.quarkus.vertx.core.runtime.config.ClusterConfiguration;
import io.quarkus.vertx.core.runtime.config.EventBusConfiguration;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.file.impl.FileResolver;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxBuilder;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.impl.VertxThread;
import io.vertx.core.impl.WorkerPool;
import io.vertx.core.spi.VertxThreadFactory;
import io.vertx.core.spi.resolver.ResolverProvider;

@Recorder
public class VertxCoreRecorder {

    static {
        System.setProperty("vertx.disableTCCL", "true");
    }

    private static final Logger LOGGER = Logger.getLogger(VertxCoreRecorder.class.getName());
    public static final String VERTX_CACHE = "vertx-cache";

    static volatile VertxSupplier vertx;

    static volatile int blockingThreadPoolSize;

    /**
     * This is a bit of a hack. In dev mode we undeploy all the verticles on restart, except
     * for this one
     */
    private static volatile String webDeploymentId;

    public Supplier<Vertx> configureVertx(VertxConfiguration config,
            LaunchMode launchMode, ShutdownContext shutdown, List<Consumer<VertxOptions>> customizers,
            ExecutorService executorProxy) {
        QuarkusExecutorFactory.sharedExecutor = executorProxy;
        if (launchMode != LaunchMode.DEVELOPMENT) {
            vertx = new VertxSupplier(launchMode, config, customizers, shutdown);
            // we need this to be part of the last shutdown tasks because closing it early (basically before Arc)
            // could cause problem to beans that rely on Vert.x and contain shutdown tasks
            shutdown.addLastShutdownTask(new Runnable() {
                @Override
                public void run() {
                    destroy();
                    QuarkusExecutorFactory.sharedExecutor = null;
                }
            });
        } else {
            if (vertx == null) {
                vertx = new VertxSupplier(launchMode, config, customizers, shutdown);
            } else if (vertx.v != null) {
                tryCleanTccl(vertx.v);
            }
            shutdown.addLastShutdownTask(new Runnable() {
                @Override
                public void run() {
                    List<CountDownLatch> latches = new ArrayList<>();
                    if (vertx.v != null) {
                        Set<String> ids = new HashSet<>(vertx.v.deploymentIDs());
                        for (String id : ids) {
                            if (!id.equals(webDeploymentId)) {
                                CountDownLatch latch = new CountDownLatch(1);
                                latches.add(latch);
                                vertx.v.undeploy(id, new Handler<AsyncResult<Void>>() {
                                    @Override
                                    public void handle(AsyncResult<Void> event) {
                                        latch.countDown();
                                    }
                                });
                            }
                        }
                        for (CountDownLatch latch : latches) {
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                LOGGER.error("Failed waiting for verticle undeploy", e);
                            }
                        }
                    }
                    QuarkusExecutorFactory.sharedExecutor = null;
                }
            });
        }
        return vertx;
    }

    private void tryCleanTccl(Vertx devModeVertx) {
        //this is a best effort attempt to clean out the old TCCL from
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        resetExecutorsClassloaderContext(extractWorkerPool(devModeVertx), cl);
        resetExecutorsClassloaderContext(extractInternalWorkerPool(devModeVertx), cl);

        EventLoopGroup group = ((VertxImpl) devModeVertx).getEventLoopGroup();
        for (EventExecutor i : group) {
            i.execute(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setContextClassLoader(cl);
                }

            });
        }

    }

    private WorkerPool extractInternalWorkerPool(Vertx devModeVertx) {
        VertxImpl vertxImpl = (VertxImpl) devModeVertx;
        final Object internalWorkerPool;
        final Field field;
        try {
            field = VertxImpl.class.getDeclaredField("internalWorkerPool");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        try {
            internalWorkerPool = field.get(vertxImpl);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return (WorkerPool) internalWorkerPool;
    }

    private WorkerPool extractWorkerPool(Vertx devModeVertx) {
        final ContextInternal ctx = (ContextInternal) devModeVertx.getOrCreateContext();
        return ctx.workerPool();
    }

    /**
     * Extract the JBoss Threads EnhancedQueueExecutor from the Vertx instance
     * and reset all threads to use the given ClassLoader.
     * This is messy as it needs to use reflection until Vertx can expose it:
     * - https://github.com/eclipse-vertx/vert.x/pull/4029
     */
    private void resetExecutorsClassloaderContext(WorkerPool workerPool, ClassLoader cl) {
        final Method executorMethod;
        try {
            executorMethod = WorkerPool.class.getDeclaredMethod("executor");
            executorMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        final Object result;
        try {
            result = executorMethod.invoke(workerPool);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        EnhancedQueueExecutor executor = (EnhancedQueueExecutor) result;
        final Thread[] runningThreads = executor.getRunningThreads();
        for (Thread t : runningThreads) {
            t.setContextClassLoader(cl);
        }
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

    public static Vertx initialize(VertxConfiguration conf, VertxOptionsCustomizer customizer, ShutdownContext shutdown,
            LaunchMode launchMode) {

        VertxOptions options = new VertxOptions();

        if (conf != null) {
            convertToVertxOptions(conf, options, true, shutdown);
        }

        // Allow extension customizers to do their thing
        if (customizer != null) {
            customizer.customize(options);
        }

        Vertx vertx;

        if (conf != null && conf.cluster != null && conf.cluster.clustered) {
            CompletableFuture<Vertx> latch = new CompletableFuture<>();
            new VertxBuilder(options)
                    .executorServiceFactory(new QuarkusExecutorFactory(conf, launchMode))
                    .init().clusteredVertx(new Handler<AsyncResult<Vertx>>() {
                        @Override
                        public void handle(AsyncResult<Vertx> ar) {
                            if (ar.failed()) {
                                latch.completeExceptionally(ar.cause());
                            } else {
                                latch.complete(ar.result());
                            }
                        }
                    });
            vertx = latch.join();
        } else {
            vertx = new VertxBuilder(options)
                    .executorServiceFactory(new QuarkusExecutorFactory(conf, launchMode))
                    .init().vertx();
        }

        vertx.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable error) {
                LOGGER.error("Uncaught exception received by Vert.x", error);
            }
        });
        return logVertxInitialization(vertx);
    }

    private static Vertx logVertxInitialization(Vertx vertx) {
        LOGGER.debugf("Vertx has Native Transport Enabled: %s", vertx.isNativeTransportEnabled());
        return vertx;
    }

    private static VertxOptions convertToVertxOptions(VertxConfiguration conf, VertxOptions options, boolean allowClustering,
            ShutdownContext shutdown) {

        if (!conf.useAsyncDNS) {
            System.setProperty(ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME, "true");
        }

        setAddressResolverOptions(conf, options);

        if (allowClustering) {
            // Order matters, as the cluster options modifies the event bus options.
            setEventBusOptions(conf, options);
            initializeClusterOptions(conf, options);
        }

        String fileCacheDir = System.getProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME);
        if (fileCacheDir == null) {
            File tmp = new File(System.getProperty("java.io.tmpdir", ".") + File.separator + VERTX_CACHE);
            if (!tmp.isDirectory()) {
                if (!tmp.mkdirs()) {
                    LOGGER.warnf("Unable to create Vert.x cache directory : %s", tmp.getAbsolutePath());
                }
                String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
                if (!os.contains("windows")) {
                    // Do not execute the following on windows.
                    if (!(tmp.setReadable(true, false) && tmp.setWritable(true, false))) {
                        LOGGER.warnf("Unable to make the Vert.x cache directory (%s) world readable and writable",
                                tmp.getAbsolutePath());
                    }
                }
            }

            File cache = getRandomDirectory(tmp);
            LOGGER.debugf("Vert.x Cache configured to: %s", cache.getAbsolutePath());
            fileCacheDir = cache.getAbsolutePath();
            if (shutdown != null) {
                shutdown.addLastShutdownTask(new Runnable() {
                    @Override
                    public void run() {
                        // Recursively delete the created directory and all the files
                        deleteDirectory(cache);
                        // We do not delete the vertx-cache directory on purpose, as it could be used concurrently by
                        // another application. In the worse case, it's just an empty directory.
                    }
                });
            }
        }

        options.setFileSystemOptions(new FileSystemOptions()
                .setFileCachingEnabled(conf.caching)
                .setFileCacheDir(fileCacheDir)
                .setClassPathResolvingEnabled(conf.classpathResolving));
        options.setWorkerPoolSize(conf.workerPoolSize);
        options.setInternalBlockingPoolSize(conf.internalBlockingPoolSize);
        blockingThreadPoolSize = conf.internalBlockingPoolSize;

        options.setBlockedThreadCheckInterval(conf.warningExceptionTime.toMillis());
        if (conf.eventLoopsPoolSize.isPresent()) {
            options.setEventLoopPoolSize(conf.eventLoopsPoolSize.getAsInt());
        } else {
            options.setEventLoopPoolSize(calculateDefaultIOThreads());
        }

        options.setMaxEventLoopExecuteTime(conf.maxEventLoopExecuteTime.toMillis());
        options.setMaxEventLoopExecuteTimeUnit(TimeUnit.MILLISECONDS);

        options.setMaxWorkerExecuteTime(conf.maxWorkerExecuteTime.toMillis());
        options.setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS);

        options.setWarningExceptionTime(conf.warningExceptionTime.toNanos());

        options.setPreferNativeTransport(conf.preferNativeTransport);

        return options;
    }

    private static File getRandomDirectory(File tmp) {
        long random = Math.abs(UUID.randomUUID().getMostSignificantBits());
        File cache = new File(tmp, Long.toString(random));
        if (cache.isDirectory()) {
            // Do not reuse an existing directory.
            return getRandomDirectory(tmp);
        }
        return cache;
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
            // Netty attaches a ThreadLocal to the main thread that can leak the QuarkusClassLoader which can be problematic in dev or test mode
            FastThreadLocal.destroy();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> problem = new AtomicReference<>();
            vertx.v.close(new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> ar) {
                    if (ar.failed()) {
                        problem.set(ar.cause());
                    }
                    latch.countDown();
                }
            });
            try {
                latch.await();
                if (problem.get() != null) {
                    throw new IllegalStateException("Error when closing Vert.x instance", problem.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Exception when closing Vert.x instance", e);
            }
            vertx = null;
        }
    }

    private static void initializeClusterOptions(VertxConfiguration conf, VertxOptions options) {
        ClusterConfiguration cluster = conf.cluster;
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

    private static void setAddressResolverOptions(VertxConfiguration conf, VertxOptions options) {
        AddressResolverConfiguration ar = conf.resolver;
        AddressResolverOptions opts = new AddressResolverOptions();
        opts.setCacheMaxTimeToLive(ar.cacheMaxTimeToLive);
        opts.setCacheMinTimeToLive(ar.cacheMinTimeToLive);
        opts.setCacheNegativeTimeToLive(ar.cacheNegativeTimeToLive);

        options.setAddressResolverOptions(opts);
    }

    public Supplier<EventLoopGroup> bossSupplier() {
        return new Supplier<EventLoopGroup>() {
            @Override
            public EventLoopGroup get() {
                vertx.get();
                return ((VertxImpl) vertx.v).getAcceptorEventLoopGroup();
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

    public ThreadFactory createThreadFactory() {
        AtomicInteger threadCount = new AtomicInteger(0);
        return runnable -> {
            VertxThread thread = VertxThreadFactory.INSTANCE.newVertxThread(runnable,
                    "executor-thread-" + threadCount.getAndIncrement(), true, 0, null);
            thread.setDaemon(true);
            return thread;
        };
    }

    public ContextHandler<Object> executionContextHandler() {
        return new ContextHandler<Object>() {
            @Override
            public Object captureContext() {
                return Vertx.currentContext();
            }

            @Override
            public void runWith(Runnable task, Object context) {
                if (context != null) {
                    // Only do context handling if it's non null
                    final ContextInternal vertxContext = (ContextInternal) context;
                    vertxContext.beginDispatch();
                    try {
                        task.run();
                    } finally {
                        vertxContext.endDispatch(null);
                    }
                } else {
                    task.run();
                }
            }
        };
    }

    public static Supplier<Vertx> recoverFailedStart(VertxConfiguration config) {
        return vertx = new VertxSupplier(LaunchMode.DEVELOPMENT, config, Collections.emptyList(), null);

    }

    static class VertxSupplier implements Supplier<Vertx> {
        final LaunchMode launchMode;
        final VertxConfiguration config;
        final VertxOptionsCustomizer customizer;
        final ShutdownContext shutdown;
        Vertx v;

        VertxSupplier(LaunchMode launchMode, VertxConfiguration config, List<Consumer<VertxOptions>> customizers,
                ShutdownContext shutdown) {
            this.launchMode = launchMode;
            this.config = config;
            this.customizer = new VertxOptionsCustomizer(customizers);
            this.shutdown = shutdown;
        }

        @Override
        public synchronized Vertx get() {
            if (v == null) {
                v = initialize(config, customizer, shutdown, launchMode);
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

    public static void setWebDeploymentId(String webDeploymentId) {
        VertxCoreRecorder.webDeploymentId = webDeploymentId;
    }

    private static void deleteDirectory(File directory) {
        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteDirectory(child);
            }
        }
        directory.delete();
    }
}
