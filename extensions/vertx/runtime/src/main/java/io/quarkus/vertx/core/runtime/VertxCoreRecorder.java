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
import io.quarkus.vertx.core.runtime.config.NativeTransportType;
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
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.core.spi.VertxThreadFactory;
import io.vertx.core.transport.Transport;

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
     * Store the set of native transports found in the classpath at build time.
     * This is used to check if the required native transport (if any) is available.
     */
    static volatile Set<String> detectedNativeTransports = Set.of();

    /**
     * Sets the list of native transports found in the classpath at build time.
     *
     * @param transports the set of transport, empty if none.
     */
    public void setDetectedNativeTransports(Set<String> transports) {
        detectedNativeTransports = transports;
    }

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
            List<String> vertxServiceProviderClassNames,
            List<String> verticleFactoryClassNames, ExecutorService executorProxy) {
        // The wrapper previously here to prevent the executor to be shutdown prematurely is moved to higher level to the io.quarkus.runtime.ExecutorRecorder
        QuarkusExecutorFactory.sharedExecutor = executorProxy;

        if (launchMode != LaunchMode.DEVELOPMENT) {
            vertx = new VertxSupplier(launchMode, vertxConfig.getValue(), new ArrayList<>(bootstrapCustomizers),
                    new ArrayList<>(optionsCustomizers), threadPoolConfig.getValue(), shutdown,
                    vertxServiceProviderClassNames, verticleFactoryClassNames);
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
                        shutdown, vertxServiceProviderClassNames, verticleFactoryClassNames);
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
            LaunchMode launchMode, List<String> vertxServiceProviderClassNames,
            List<String> verticleFactoryClassNames) {

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
        if (vertxServiceProviderClassNames == null) {
            vertxServiceProviderClassNames = Collections.emptyList();
        }
        if (verticleFactoryClassNames == null) {
            verticleFactoryClassNames = Collections.emptyList();
        }

        var bootstrap = VertxBootstrap.create()
                .options(options.setDisableTCCL(true))
                .executorServiceFactory(new QuarkusExecutorFactory(conf, launchMode))
                .threadFactory(vertxThreadFactory);

        if (launchMode != LaunchMode.DEVELOPMENT) {
            List<VertxServiceProvider> vertxServiceProviders = instantiateServices(vertxServiceProviderClassNames,
                    VertxServiceProvider.class);
            bootstrap.serviceProviders(vertxServiceProviders);
        }

        List<VerticleFactory> verticleFactories = instantiateServices(verticleFactoryClassNames, VerticleFactory.class);
        bootstrap.verticleFactories(verticleFactories);

        if (customizer != null) {
            customizer.customize(bootstrap);
        }

        if (conf != null) {
            NativeTransportType transportType = conf.nativeTransportType();
            if (transportType != NativeTransportType.AUTO) {
                io.vertx.core.transport.Transport requested = switch (transportType) {
                    case EPOLL -> Transport.EPOLL;
                    case KQUEUE -> Transport.KQUEUE;
                    case IO_URING -> Transport.IO_URING;
                    default -> null;
                };
                if (requested != null && requested.available()) {
                    bootstrap.transport(requested.implementation());
                }
            } else if (conf.preferNativeTransport()) {
                // VertxBootstrap does not check VertxOptions.preferNativeTransport,
                // so we mirror what VertxBuilder does: auto-detect the best transport.
                Transport nativeTransport = Transport.nativeTransport();
                if (nativeTransport != null && nativeTransport.available()) {
                    bootstrap.transport(nativeTransport.implementation());
                }
            }
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

        return logVertxInitialization(vertx, conf);
    }

    private static <T> List<T> instantiateServices(List<String> classNames, Class<T> serviceClass) {
        List<T> services = new ArrayList<>();
        for (String className : classNames) {
            try {
                Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                services.add(serviceClass.cast(clazz.getDeclaredConstructor().newInstance()));
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to instantiate " + serviceClass.getSimpleName() + " class: " + className, e);
            }
        }
        return services;
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

    /**
     * Validate and Log native transport selection.
     * <p>
     * The validation depends on the value of prefer-native-transport, native-transport-type and native-transport-required:
     *
     * <ol>
     * <li>If prefer-native-transport is set to false, it will use NIO.</li>
     * <li>If prefer-native-transport is set to true, and native-transport-type is set to auto, it will pick the most
     * appropriate one. If no native transport is found on the classpath, it will use NIO</li>
     * <li>If prefer-native-transport is set to true and native-transport-type selects an explicit transport, it will use
     * this transport. If not found, an exception is thrown.</li>
     * </ol>
     *
     * @param vertx the vert.x instance
     * @param conf the configuration
     * @return the vert.x instance
     */
    private static Vertx logVertxInitialization(Vertx vertx, VertxConfiguration conf) {
        if (conf == null) {
            LOGGER.debugf("Vertx has Native Transport Enabled: %s", vertx.isNativeTransportEnabled());
            return vertx;
        }

        NativeTransportType requestedType = conf.nativeTransportType();
        boolean preferNative = conf.preferNativeTransport() || requestedType != NativeTransportType.AUTO;
        boolean required = conf.nativeTransportRequired();

        if (!preferNative) {
            LOGGER.debugf("Vertx has Native Transport Enabled: %s", vertx.isNativeTransportEnabled());
            return vertx;
        }

        if (vertx.isNativeTransportEnabled()) {
            if (requestedType != NativeTransportType.AUTO) {
                Transport requestedTransport = switch (requestedType) {
                    case EPOLL -> Transport.EPOLL;
                    case KQUEUE -> Transport.KQUEUE;
                    case IO_URING -> Transport.IO_URING;
                    default -> null;
                };
                if (requestedTransport != null && requestedTransport.available()) {
                    LOGGER.infof("Native transport enabled: %s", requestedType.transportName);
                } else if (requestedTransport != null) {
                    // We have an explicit request for a given transport, but it's not available
                    String activeTransport = detectActiveTransportName();
                    String msg = String.format(
                            "Requested native transport '%s' but '%s' was loaded instead.",
                            requestedType.transportName, activeTransport);
                    if (required) {
                        throw new IllegalStateException(msg);
                    }
                    LOGGER.warnf(msg);
                }
            } else {
                // auto
                String activeTransport = detectActiveTransportName();
                LOGGER.infof("Native transport enabled: %s", activeTransport);
            }
        } else {
            Set<String> detected = detectedNativeTransports;
            String msg;
            if (detected.isEmpty()) {
                msg = "Native transport was requested but no native transport dependency was found. "
                        + "Add io.netty:netty-transport-native-epoll (Linux) or "
                        + "io.netty:netty-transport-native-kqueue (macOS) to your project. "
                        + "See the Native Transport Reference guide for details.";
            } else {
                Throwable cause = vertx.unavailableNativeTransportCause();
                String causeMsg = cause != null ? " Cause: " + cause.getMessage() : "";
                msg = String.format(
                        "Native transport was requested and %s was found on the classpath, "
                                + "but it failed to load on this platform: %s",
                        detected, causeMsg);
            }
            if (required) {
                throw new IllegalStateException(msg);
            }
            LOGGER.warn(msg);
        }

        return vertx;
    }

    private static String detectActiveTransportName() {
        try {
            var epoll = Transport.EPOLL;
            if (epoll != null && epoll.available()) {
                return NativeTransportType.EPOLL.transportName;
            }
        } catch (Throwable ignored) {
        }
        try {
            var kqueue = Transport.KQUEUE;
            if (kqueue != null && kqueue.available()) {
                return NativeTransportType.KQUEUE.transportName;
            }
        } catch (Throwable ignored) {
        }
        try {
            var iouring = Transport.IO_URING;
            if (iouring != null && iouring.available()) {
                return NativeTransportType.IO_URING.transportName;
            }
        } catch (Throwable ignored) {
            // Ignore the loading issue, it will be made available using `unavailableNativeTransportCause`
        }
        return "unknown";
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

        boolean preferNative = conf.preferNativeTransport() || conf.nativeTransportType() != NativeTransportType.AUTO;
        options.setPreferNativeTransport(preferNative);

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
        opts.setCacheMaxTimeToLive((int) ar.cacheMaxTimeToLive().toSeconds());
        opts.setCacheMinTimeToLive((int) ar.cacheMinTimeToLive().toSeconds());
        opts.setCacheNegativeTimeToLive((int) ar.cacheNegativeTimeToLive().toSeconds());
        opts.setMaxQueries(ar.maxQueries());
        opts.setQueryTimeout(ar.queryTimeout().toMillis());
        opts.setHostsRefreshPeriod((int) ar.hostRefreshPeriod().toMillis());
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
                io.vertx.core.Context ctx = Vertx.currentContext();
                if (ctx == null) {
                    return null;
                }
                // Snapshot local context data and MDC at capture time (on the submitting
                // thread) so that concurrent modifications to the original context after
                // submission don't affect the dispatched task.
                ConcurrentHashMap<String, Object> localSnapshot = new ConcurrentHashMap<>(
                        VertxContext.localContextData(ctx));
                ConcurrentHashMap<String, Object> mdcSnapshot = new ConcurrentHashMap<>(
                        VertxMDC.MDC_LOCAL.get(ctx, ConcurrentHashMap::new));
                return new Object[] { ctx, localSnapshot, mdcSnapshot };
            }

            @Override
            public void runWith(Runnable task, Object context) {
                if (context == null) {
                    task.run();
                    return;
                }
                Object[] captured = (Object[]) context;
                ContextInternal vertxContext = (ContextInternal) captured[0];
                ConcurrentHashMap<String, Object> localSnapshot = (ConcurrentHashMap<String, Object>) captured[1];
                ConcurrentHashMap<String, Object> mdcSnapshot = (ConcurrentHashMap<String, Object>) captured[2];

                ContextInternal currentContext = (ContextInternal) Vertx.currentContext();
                if (vertxContext != currentContext) {
                    // Each dispatched task gets its own duplicate context to prevent
                    // concurrent threads from clobbering each other's local context data
                    ContextInternal taskContext = vertxContext.duplicate();
                    VertxContext.localContextData(taskContext).putAll(localSnapshot);
                    VertxMDC.MDC_LOCAL.get(taskContext, ConcurrentHashMap::new).putAll(mdcSnapshot);

                    if (ignoredKeys != null && containsIgnoredKey(ignoredKeys, localSnapshot)) {
                        ignoredKeys.forEach(VertxContext.localContextData(taskContext)::remove);
                    }
                    VertxContextSafetyToggle.setContextSafe(taskContext, true);
                    taskContext.beginDispatch();
                    try {
                        task.run();
                    } finally {
                        taskContext.endDispatch(currentContext);
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
                threadPoolConfig, null, List.of(),
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
        final List<String> vertxServiceProviderClassNames;
        final List<String> verticleFactoryClassNames;
        Vertx v;

        VertxSupplier(LaunchMode launchMode, VertxConfiguration config,
                List<Consumer<VertxBootstrap>> bootstrapCustomizers,
                List<Consumer<VertxOptions>> optionCustomizers,
                ThreadPoolConfig threadPoolConfig,
                ShutdownContext shutdown,
                List<String> vertxServiceProviderClassNames, List<String> verticleFactoryClassNames) {
            this.launchMode = launchMode;
            this.config = config;
            this.customizer = new VertxCustomizer(bootstrapCustomizers, optionCustomizers);
            this.threadPoolConfig = threadPoolConfig;
            this.shutdown = shutdown;
            this.vertxServiceProviderClassNames = vertxServiceProviderClassNames;
            this.verticleFactoryClassNames = verticleFactoryClassNames;
        }

        @Override
        public synchronized Vertx get() {
            if (v == null) {
                v = initialize(config, customizer, threadPoolConfig, shutdown, launchMode, vertxServiceProviderClassNames,
                        verticleFactoryClassNames);
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
