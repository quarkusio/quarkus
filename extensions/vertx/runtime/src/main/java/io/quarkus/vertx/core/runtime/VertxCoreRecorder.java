package io.quarkus.vertx.core.runtime;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.jboss.threads.ContextHandler;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.FastThreadLocal;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.IOThreadDetector;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.shutdown.ShutdownConfig;
import io.quarkus.vertx.core.runtime.config.AddressResolverConfiguration;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.vertx.mdc.provider.LateBoundMDCProvider;
import io.quarkus.vertx.runtime.VertxCurrentContextFactory;
import io.quarkus.vertx.runtime.jackson.QuarkusJacksonFactory;
import io.smallrye.common.cpu.ProcessorInfo;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.impl.SysProps;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.impl.VertxThread;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.VertxBootstrap;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.core.spi.VertxThreadFactory;

@Recorder
public class VertxCoreRecorder {

    private static final String LOGGER_FACTORY_NAME_SYS_PROP = "vertx.logger-delegate-factory-class-name";

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

    /**
     * All current dev mode threads, accessed under lock
     * <p>
     * This allows them to have their TCCL updated on restart
     */
    private static final Set<Thread> devModeThreads = new HashSet<>();
    /**
     * The class loader to use for new threads in dev mode. On dev mode restart this must be updated under the
     * {@link #devModeThreads} lock, to
     * avoid race conditions.
     */
    private static volatile ClassLoader currentDevModeNewThreadCreationClassLoader;

    private final RuntimeValue<VertxConfiguration> vertxConfig;
    private final RuntimeValue<ThreadPoolConfig> threadPoolConfig;
    private final RuntimeValue<ShutdownConfig> shutdownConfig;

    public VertxCoreRecorder(RuntimeValue<VertxConfiguration> vertxConfig, RuntimeValue<ThreadPoolConfig> threadPoolConfig,
            RuntimeValue<ShutdownConfig> shutdownConfig) {
        this.vertxConfig = vertxConfig;
        this.threadPoolConfig = threadPoolConfig;
        this.shutdownConfig = shutdownConfig;
    }

    public Supplier<Vertx> configureVertx(LaunchMode launchMode, ShutdownContext shutdown,
            List<Consumer<VertxBootstrap>> bootstrapCustomizers, List<Consumer<VertxOptions>> optionsCustomizers,
            List<VerticleFactory> verticleFactories, ExecutorService executorProxy) {
        // The wrapper previously here to prevent the executor to be shutdown prematurely is moved to higher level to the io.quarkus.runtime.ExecutorRecorder
        QuarkusExecutorFactory.sharedExecutor = executorProxy;

        if (launchMode != LaunchMode.DEVELOPMENT) {
            vertx = new VertxSupplier(launchMode, vertxConfig.getValue(), new ArrayList<>(bootstrapCustomizers),
                    new ArrayList<>(optionsCustomizers),
                    threadPoolConfig.getValue(), shutdown,
                    verticleFactories);
            // we need this to be part of the last shutdown tasks because closing it early (basically before Arc)
            // could cause problem to beans that rely on Vert.x and contain shutdown tasks
            shutdown.addLastShutdownTask(new Runnable() {
                @Override
                public void run() {
                    destroy();
                    QuarkusExecutorFactory.sharedExecutor = null;
                    currentDevModeNewThreadCreationClassLoader = null;
                }
            });
        } else {
            if (vertx == null) {
                vertx = new VertxSupplier(launchMode, vertxConfig.getValue(), new ArrayList<>(bootstrapCustomizers),
                        new ArrayList<>(optionsCustomizers),
                        threadPoolConfig.getValue(),
                        shutdown, verticleFactories);
            } else if (vertx.v != null) {
                tryCleanTccl();
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
                                vertx.v.undeploy(id).onComplete(new Handler<AsyncResult<Void>>() {
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

    private void tryCleanTccl() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        synchronized (devModeThreads) {
            currentDevModeNewThreadCreationClassLoader = cl;
            // Collect terminated threads to remove them from the set. It will avoid iterating over them in the future.
            List<Thread> terminated = new ArrayList<>();
            for (var t : devModeThreads) {
                if (t.getState() == Thread.State.TERMINATED) {
                    terminated.add(t);
                } else {
                    t.setContextClassLoader(cl);
                }
            }
            terminated.forEach(devModeThreads::remove);
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
            vertx.get().close().onComplete(new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> event) {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                synchronized (devModeThreads) {
                    devModeThreads.clear();
                    currentDevModeNewThreadCreationClassLoader = null;
                }
            }
        }
    }

    public static Supplier<Vertx> getVertx() {
        return vertx;
    }

    public static Vertx initialize(VertxConfiguration conf, VertxCustomizer customizer,
            ThreadPoolConfig threadPoolConfig, ShutdownContext shutdown,
            LaunchMode launchMode, List<VerticleFactory> verticleFactories) {

        VertxOptions options = new VertxOptions();

        if (conf != null) {
            convertToVertxOptions(conf, options, threadPoolConfig, shutdown);
        }

        if (customizer != null) {
            customizer.customize(options);
        }

        Vertx vertx;

        Optional<ClassLoader> nonDevModeTccl = setupThreadFactoryTccl(launchMode);
        VertxThreadFactory vertxThreadFactory = new VertxThreadFactory() {
            @Override
            public VertxThread newVertxThread(Runnable target, String name, boolean worker, long maxExecTime,
                    TimeUnit maxExecTimeUnit) {
                return createVertxThread(target, name, worker, maxExecTime, maxExecTimeUnit, launchMode, nonDevModeTccl);
            }
        };
        var bootstrap = VertxBootstrap.create()
                .options(options.setDisableTCCL(true))
                .executorServiceFactory(new QuarkusExecutorFactory(conf, launchMode))
                .threadFactory(vertxThreadFactory);

        if (customizer != null) {
            customizer.customize(bootstrap);
        }

        vertx = bootstrap.init().vertx();

        for (VerticleFactory verticleFactory : verticleFactories) {
            vertx.registerVerticleFactory(verticleFactory);
        }

        vertx.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable error) {
                LOGGER.error("Uncaught exception received by Vert.x", error);
            }
        });

        LateBoundMDCProvider.setMDCProviderDelegate(VertxMDC.INSTANCE);

        return logVertxInitialization(vertx);
    }

    /**
     * Depending on the launch mode we may need to handle the TCCL differently.
     * <p>
     * For dev mode it can change, so we don't want to capture the original TCCL (as this would be a leak). For other modes we
     * just want a fixed TCCL, and leaks are not an issue.
     *
     * @param launchMode The launch mode
     * @return The ClassLoader if we are not running in dev mode
     */
    private static Optional<ClassLoader> setupThreadFactoryTccl(LaunchMode launchMode) {
        Optional<ClassLoader> nonDevModeTccl;
        if (launchMode == LaunchMode.DEVELOPMENT) {
            currentDevModeNewThreadCreationClassLoader = Thread.currentThread().getContextClassLoader();
            nonDevModeTccl = Optional.empty(); //in dev mode we don't want to capture the original TCCL to stop a leak
        } else {
            nonDevModeTccl = Optional.of(Thread.currentThread().getContextClassLoader());
        }
        return nonDevModeTccl;
    }

    private static VertxThread createVertxThread(Runnable target, String name, boolean worker, long maxExecTime,
            TimeUnit maxExecTimeUnit, LaunchMode launchMode, Optional<ClassLoader> nonDevModeTccl) {
        var thread = VertxThreadFactory.INSTANCE.newVertxThread(target, name, worker, maxExecTime, maxExecTimeUnit);
        if (launchMode == LaunchMode.DEVELOPMENT) {
            synchronized (devModeThreads) {
                setNewThreadTccl(thread);
                devModeThreads.add(thread);
            }
        } else {
            thread.setContextClassLoader(nonDevModeTccl.get());
        }
        return thread;
    }

    private static Vertx logVertxInitialization(Vertx vertx) {
        LOGGER.debugf("Vertx has Native Transport Enabled: %s", vertx.isNativeTransportEnabled());
        return vertx;
    }

    private static VertxOptions convertToVertxOptions(VertxConfiguration conf, VertxOptions options,
            ThreadPoolConfig threadPoolConfig,
            ShutdownContext shutdown) {

        if (!conf.useAsyncDNS()) {
            System.setProperty(SysProps.DISABLE_DNS_RESOLVER.name, "true");
        }

        setAddressResolverOptions(conf, options);

        FileSystemOptions fileSystemOptions = new FileSystemOptions()
                .setFileCachingEnabled(conf.caching())
                .setClassPathResolvingEnabled(conf.classpathResolving());

        String fileCacheDir = System.getProperty(SysProps.FILE_CACHE_DIR.name);
        if (fileCacheDir == null) {
            fileCacheDir = conf.cacheDirectory().orElse(null);
        }

        if (fileCacheDir == null) {
            // If not set, make sure we can create a directory in the temp directory.
            File tmp = new File(System.getProperty("java.io.tmpdir", ".") + File.separator + VERTX_CACHE);
            boolean cacheDirRequired = conf.caching() || conf.classpathResolving();
            if (!tmp.isDirectory() && cacheDirRequired) {
                if (!tmp.mkdirs()) {
                    LOGGER.warnf("Unable to create Vert.x cache directory : %s", tmp.getAbsolutePath());
                }
                String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
                if (!os.contains("windows")) {
                    // Do not execute the following on Windows.
                    if (!(tmp.setReadable(true, false) && tmp.setWritable(true, false))) {
                        LOGGER.warnf("Unable to make the Vert.x cache directory (%s) world readable and writable",
                                tmp.getAbsolutePath());
                    }
                }
            }

            if (cacheDirRequired) {
                File cache = getRandomDirectory(tmp);
                LOGGER.debugf("Vert.x Cache configured to: %s", cache.getAbsolutePath());
                fileSystemOptions.setFileCacheDir(cache.getAbsolutePath());
                fileSystemOptions.setFileCacheDirAsExactPath(true);
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
        } else {
            fileSystemOptions.setFileCacheDir(fileCacheDir);
            fileSystemOptions.setFileCacheDirAsExactPath(true);
        }

        options.setFileSystemOptions(fileSystemOptions);
        options.setWorkerPoolSize(ExecutorRecorder.getMaxSize(threadPoolConfig));
        options.setInternalBlockingPoolSize(conf.internalBlockingPoolSize());
        blockingThreadPoolSize = conf.internalBlockingPoolSize();

        options.setBlockedThreadCheckInterval(conf.blockedThreadCheckInterval().toMillis());
        if (conf.eventLoopsPoolSize().isPresent()) {
            options.setEventLoopPoolSize(conf.eventLoopsPoolSize().getAsInt());
        } else {
            options.setEventLoopPoolSize(calculateDefaultIOThreads());
        }

        options.setMaxEventLoopExecuteTime(conf.maxEventLoopExecuteTime().toMillis());
        options.setMaxEventLoopExecuteTimeUnit(TimeUnit.MILLISECONDS);

        options.setMaxWorkerExecuteTime(conf.maxWorkerExecuteTime().toMillis());
        options.setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS);

        options.setWarningExceptionTime(conf.warningExceptionTime().toNanos());

        options.setPreferNativeTransport(conf.preferNativeTransport());

        options.setDisableTCCL(true);
        options.setUseDaemonThread(false);

        return options;
    }

    private static File getRandomDirectory(File tmp) {
        File cache = new File(tmp, Long.toString(new Random().nextLong()));
        if (cache.isDirectory()) {
            // Do not reuse an existing directory.
            return getRandomDirectory(tmp);
        }
        return cache;
    }

    private static int calculateDefaultIOThreads() {
        //we only allow one event loop per 10mb of ram at the most
        //it's hard to say what this number should be, but it is also obvious
        //that for constrained environments we don't want a lot of event loops
        //lets start with 10mb and adjust as needed
        //We used to recommend a default of twice the number of cores,
        //but more recent developments seem to suggest matching the number of cores 1:1
        //being a more reasonable default. It also saves memory.
        int recommended = ProcessorInfo.availableProcessors();
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
            vertx.v.close().onComplete(new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> ar) {
                    if (ar.failed()) {
                        problem.set(ar.cause());
                    }
                    latch.countDown();
                }
            });
            try {
                // Use configured shutdown timeout or default to 10 seconds
                long timeoutMillis = shutdownConfig.getValue().timeout()
                        .map(duration -> duration.toMillis())
                        .orElse(10000L);
                boolean completed = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
                if (!completed) {
                    LOGGER.warn("Vert.x shutdown timed out after " + timeoutMillis + "ms");
                }
                if (problem.get() != null) {
                    throw new IllegalStateException("Error when closing Vert.x instance", problem.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Exception when closing Vert.x instance", e);
            }
            VertxMDC.INSTANCE.clear();
            LateBoundMDCProvider.setMDCProviderDelegate(null);
            vertx = null;
        }
    }

    private static void setAddressResolverOptions(VertxConfiguration conf, VertxOptions options) {
        AddressResolverConfiguration ar = conf.resolver();
        AddressResolverOptions opts = new AddressResolverOptions();
        opts.setCacheMaxTimeToLive(ar.cacheMaxTimeToLive());
        opts.setCacheMinTimeToLive(ar.cacheMinTimeToLive());
        opts.setCacheNegativeTimeToLive(ar.cacheNegativeTimeToLive());
        opts.setMaxQueries(ar.maxQueries());
        opts.setQueryTimeout(ar.queryTimeout().toMillis());
        opts.setHostsRefreshPeriod(ar.hostRefreshPeriod());
        opts.setOptResourceEnabled(ar.optResourceEnabled());
        opts.setRdFlag(ar.rdFlag());
        opts.setNdots(ar.ndots());
        opts.setRoundRobinInetAddress(ar.roundRobinInetAddress());

        if (ar.hostsPath().isPresent()) {
            opts.setHostsPath(ar.hostsPath().get());
        }

        if (ar.servers().isPresent()) {
            opts.setServers(ar.servers().get());
        }

        if (ar.searchDomains().isPresent()) {
            opts.setSearchDomains(ar.searchDomains().get());
        }

        if (ar.rotateServers().isPresent()) {
            opts.setRotateServers(ar.rotateServers().get());
        }

        options.setAddressResolverOptions(opts);
    }

    public Supplier<EventLoopGroup> bossSupplier() {
        return new Supplier<EventLoopGroup>() {
            @Override
            public EventLoopGroup get() {
                vertx.get();
                return ((VertxImpl) vertx.v).acceptorEventLoopGroup();
            }
        };
    }

    public Supplier<EventLoopGroup> mainSupplier() {
        return new Supplier<EventLoopGroup>() {
            @Override
            public EventLoopGroup get() {
                return ((VertxImpl) vertx.v).nettyEventLoopGroup();
            }
        };
    }

    public Supplier<Integer> calculateEventLoopThreads() {
        int threads;
        if (vertxConfig.getValue().eventLoopsPoolSize().isPresent()) {
            threads = vertxConfig.getValue().eventLoopsPoolSize().getAsInt();
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

    public ThreadFactory createThreadFactory(LaunchMode launchMode) {
        Optional<ClassLoader> nonDevModeTccl = setupThreadFactoryTccl(launchMode);
        AtomicInteger threadCount = new AtomicInteger(0);
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                VertxThread thread = createVertxThread(runnable,
                        "executor-thread-" + threadCount.getAndIncrement(), true, 0, null, launchMode, nonDevModeTccl);
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    public void resetMapper(ShutdownContext shutdown) {
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                QuarkusJacksonFactory.reset();
            }
        });
    }

    private static void setNewThreadTccl(VertxThread thread) {
        ClassLoader cl = VertxCoreRecorder.currentDevModeNewThreadCreationClassLoader;
        if (cl == null) {
            //can happen if a thread is created after shutdown is initiated
            //should be super rare, but might as well handle it properly
            cl = VertxCoreRecorder.class.getClassLoader();
        }
        thread.setContextClassLoader(cl);
    }

    public RuntimeValue<List<String>> getIgnoredArcContextKeysSupplier() {
        final VertxCurrentContextFactory currentContextFactory = (VertxCurrentContextFactory) Arc.container()
                .getCurrentContextFactory();
        return new RuntimeValue<>(currentContextFactory.keys());
    }

    public ContextHandler<Object> executionContextHandler(List<RuntimeValue<List<String>>> ignoredKeysSuppliers) {
        final List<String> ignoredKeys;
        if (ignoredKeysSuppliers.isEmpty()) {
            ignoredKeys = null;
        } else {
            ignoredKeys = new ArrayList<>();
            for (RuntimeValue<List<String>> ignoredKeysSupplier : ignoredKeysSuppliers) {
                ignoredKeys.addAll(ignoredKeysSupplier.getValue());
            }
        }
        return new ContextHandler<Object>() {
            @Override
            public Object captureContext() {
                return Vertx.currentContext();
            }

            @Override
            public void runWith(Runnable task, Object context) {
                ContextInternal currentContext = (ContextInternal) Vertx.currentContext();
                // Only do context handling if it's non-null
                if (context != null && context != currentContext) {
                    ContextInternal vertxContext = (ContextInternal) context;
                    // The CDI contexts must not be propagated
                    // First test if VertxCurrentContextFactory is actually used
                    if (ignoredKeys != null) {
                        // Remove ignored keys from the main context
                        ConcurrentMap<String, Object> local = VertxContext.localContextData(vertxContext);
                        if (containsIgnoredKey(ignoredKeys, local)) {
                            var dup = vertxContext.duplicate();
                            ConcurrentHashMap<String, Object> data = VertxContext.localContextData(dup);
                            data.putAll(local);
                            ignoredKeys.forEach(data::remove);
                            VertxContextSafetyToggle.setContextSafe(vertxContext, true);

                            // Copy the MDC
                            ConcurrentHashMap<String, Object> map = VertxMDC.MDC_LOCAL.get(vertxContext,
                                    ConcurrentHashMap::new);
                            VertxMDC.MDC_LOCAL.get(dup, ConcurrentHashMap::new).putAll(map);
                        }
                    }
                    vertxContext.beginDispatch();
                    try {
                        task.run();
                    } finally {
                        vertxContext.endDispatch(currentContext);
                    }
                } else {
                    task.run();
                }
            }

            private boolean containsIgnoredKey(List<String> keys, Map<String, Object> localContextData) {
                if (keys.isEmpty()) {
                    return false;
                }
                if (keys.size() == 1) {
                    // Very often there will be only one key used
                    return localContextData.containsKey(keys.get(0));
                } else {
                    for (String key : keys) {
                        if (localContextData.containsKey(key)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    public static Supplier<Vertx> recoverFailedStart(VertxConfiguration config, ThreadPoolConfig threadPoolConfig) {
        return vertx = new VertxSupplier(LaunchMode.DEVELOPMENT, config, Collections.emptyList(), Collections.emptyList(),
                threadPoolConfig, null,
                List.of());

    }

    public void configureQuarkusLoggerFactory() {
        String loggerClassName = System.getProperty(LOGGER_FACTORY_NAME_SYS_PROP);
        if (loggerClassName == null) {
            System.setProperty(LOGGER_FACTORY_NAME_SYS_PROP, VertxLogDelegateFactory.class.getName());
        }
    }

    static class VertxSupplier implements Supplier<Vertx> {
        final LaunchMode launchMode;
        final VertxConfiguration config;
        final VertxCustomizer customizer;
        final ThreadPoolConfig threadPoolConfig;
        final ShutdownContext shutdown;
        final List<VerticleFactory> verticleFactories;
        Vertx v;

        VertxSupplier(LaunchMode launchMode, VertxConfiguration config,
                List<Consumer<VertxBootstrap>> bootstrapCustomizers,
                List<Consumer<VertxOptions>> optionCustomizers,
                ThreadPoolConfig threadPoolConfig,
                ShutdownContext shutdown,
                List<VerticleFactory> verticleFactories) {
            this.launchMode = launchMode;
            this.config = config;
            this.customizer = new VertxCustomizer(bootstrapCustomizers, optionCustomizers);
            this.threadPoolConfig = threadPoolConfig;
            this.shutdown = shutdown;
            this.verticleFactories = verticleFactories;
        }

        @Override
        public synchronized Vertx get() {
            if (v == null) {
                v = initialize(config, customizer, threadPoolConfig, shutdown, launchMode, verticleFactories);
            }
            return v;
        }
    }

    public static class VertxCustomizer {
        private final List<Consumer<VertxBootstrap>> bootstrapCustomizers;
        private final List<Consumer<VertxOptions>> optionCustomizers;

        VertxCustomizer(List<Consumer<VertxBootstrap>> bootstrapCustomizers,
                List<Consumer<VertxOptions>> optionCustomizers) {
            this.bootstrapCustomizers = bootstrapCustomizers;
            this.optionCustomizers = optionCustomizers;
            // Append runtime customizers at the end of the list.
            if (Arc.container() != null) {
                List<InstanceHandle<io.quarkus.vertx.VertxOptionsCustomizer>> instances = Arc.container()
                        .listAll(io.quarkus.vertx.VertxOptionsCustomizer.class);
                for (InstanceHandle<io.quarkus.vertx.VertxOptionsCustomizer> customizer : instances) {
                    optionCustomizers.add(customizer.get());
                }

                // No Runtime customization of the VertxBootstrap, it's an internal Vert.x API.
            }
        }

        VertxOptions customize(VertxOptions options) {
            for (Consumer<VertxOptions> x : optionCustomizers) {
                x.accept(options);
            }
            return options;
        }

        VertxBootstrap customize(VertxBootstrap bootstrap) {
            for (Consumer<VertxBootstrap> x : bootstrapCustomizers) {
                x.accept(bootstrap);
            }
            return bootstrap;
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

    public void wrapMainExecutorForMutiny(ScheduledExecutorService service) {
        VertxTimerAwareScheduledExecutorService wrapper = new VertxTimerAwareScheduledExecutorService(service);
        Infrastructure.setDefaultExecutor(wrapper, false);
    }
}
