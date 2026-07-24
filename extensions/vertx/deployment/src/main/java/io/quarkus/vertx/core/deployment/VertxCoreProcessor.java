package io.quarkus.vertx.core.deployment;

import static io.quarkus.arc.processor.DotNames.APPLICATION_SCOPED;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogManager;
import org.jboss.threads.ContextHandler;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ContextHandlerBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.IOThreadDetectorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.ThreadFactoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.mutiny.deployment.MutinyRuntimeInitBuildItem;
import io.quarkus.netty.deployment.EventLoopSupplierBuildItem;
import io.quarkus.runtime.IOThreadDetector;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.VertxOptionsCustomizer;
import io.quarkus.vertx.VertxSupplier;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.VertxLogDelegateFactory;
import io.quarkus.vertx.core.runtime.config.NativeTransportMode;
import io.quarkus.vertx.core.runtime.config.NativeTransportType;
import io.quarkus.vertx.core.runtime.config.VertxBuildTimeConfig;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.quarkus.vertx.core.runtime.context.SafeVertxContextInterceptor;
import io.quarkus.vertx.deployment.VertxBuildConfig;
import io.quarkus.vertx.deployment.spi.VertxBootstrapConsumerBuildItem;
import io.quarkus.vertx.deployment.spi.VertxOptionsConsumerBuildItem;
import io.quarkus.vertx.mdc.provider.LateBoundMDCProvider;
import io.quarkus.vertx.runtime.jackson.QuarkusJacksonFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.SysProps;
import io.vertx.core.internal.VertxBootstrap;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.core.spi.VertxServiceProvider;

class VertxCoreProcessor {

    private static final Logger log = Logger.getLogger(VertxCoreProcessor.class);

    private static final Set<String> BLOCKED_THREAD_LOGGER_NAMES = Set.of(
            "io.vertx.core.impl.BlockedThreadChecker", // Vert.x 4.2-
            "io.vertx.core.impl.btc.BlockedThreadChecker" // Vert.x 4.3+
    );

    @BuildStep
    AdditionalBeanBuildItem registerSafeDuplicatedContextInterceptor() {
        return new AdditionalBeanBuildItem(SafeVertxContextInterceptor.class.getName());
    }

    @BuildStep
    NativeImageConfigBuildItem build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources) {
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(VertxLogDelegateFactory.class.getName()).methods().build());
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(LateBoundMDCProvider.class.getName()).methods().fields().build());
        nativeImageResources.produce(new NativeImageResourceBuildItem("META-INF/services/org.jboss.logmanager.MDCProvider"));
        return NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("io.vertx.core.impl.buffer.VertxByteBufAllocator")
                .addRuntimeInitializedClass("io.vertx.core.http.impl.tcp.VertxHttp2ClientUpgradeCodec")
                .addNativeImageSystemProperty(SysProps.DISABLE_DNS_RESOLVER.name, "true")
                .addNativeImageSystemProperty("vertx.logger-delegate-factory-class-name",
                        VertxLogDelegateFactory.class.getName())
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    EventLoopCountBuildItem eventLoopCount(VertxCoreRecorder recorder, ActionBuilder action) {
        // service registration for the dependency graph
        action
                .forService(IntSupplier.class, "io.quarkus.vertx.event-loop-count")
                .atPhase(Phase.INFRASTRUCTURE)
                .require(VertxConfiguration.class)
                .action((ctx, config) -> {
                    int threads = config.eventLoopsPoolSize().isPresent()
                            ? config.eventLoopsPoolSize().getAsInt()
                            : VertxCoreRecorder.calculateDefaultIOThreads();
                    return () -> threads;
                });
        // recorder bridge for the build item (until consumers are converted)
        return new EventLoopCountBuildItem(recorder.calculateEventLoopThreads());
    }

    @BuildStep
    LogCleanupFilterBuildItem cleanupVertxWarnings() {
        return new LogCleanupFilterBuildItem("io.vertx.core.impl.ContextImpl", "You have disabled TCCL checks");
    }

    @BuildStep
    LogCategoryBuildItem preventLoggerContention() {
        //Prevent the Logging warning about the TCCL checks being disabled to be logged;
        //this is similar to #cleanupVertxWarnings but prevents it by changing the level:
        // it takes advantage of the fact that there is a single other log in this class,
        // and it happens to be at error level.
        //This is more effective than the LogCleanupFilterBuildItem as we otherwise have
        //contention since this message could be logged very frequently.
        return new LogCategoryBuildItem("io.vertx.core.impl.ContextImpl", Level.ERROR);
    }

    @BuildStep
    IOThreadDetectorBuildItem ioThreadDetector(ActionBuilder action) {
        action
                .forService(IOThreadDetector.class)
                .atPhase(Phase.STATIC_INIT)
                .action(ctx -> Context::isOnEventLoopThread);
        return new IOThreadDetectorBuildItem(
                action.staticInitServiceAsRecorderValue(IOThreadDetector.class));
    }

    /**
     * Register a static-init service whose stop handler cleans up Netty's
     * {@code InternalThreadLocalMap} from the main thread.
     * <p>
     * Netty attaches a thread-local map to non-Netty threads (like the main
     * thread) whenever they interact with Netty. This map holds strong
     * references to classes loaded by the QuarkusClassLoader, preventing
     * classloader garbage collection in dev/test mode.
     * <p>
     * The cleanup must run after <em>all</em> runtime-init services have
     * stopped — including ArC, whose bean destruction callbacks may touch
     * Netty. By placing the cleanup in a static-init service, it runs
     * during the static-init graph's stop cascade, which executes after
     * the entire runtime-init graph has stopped.
     * <p>
     * <b>Future considerations:</b>
     * <ul>
     * <li><b>Implicit stop-ordering from synthetic bean registration:</b>
     * When a service registers a synthetic CDI bean, the framework could
     * automatically infer a stop-ordering edge: ArC must destroy that
     * bean's dependency subgraph before the backing service stops. This
     * would eliminate the need for this workaround.</li>
     * <li><b>Service-scoped CDI contexts:</b> Instead of
     * {@code @ApplicationScoped}, service-backed synthetic beans could
     * live in a custom scope tied to the service graph. The scope would
     * end when the backing service stops, triggering bean destruction
     * at the right time.</li>
     * </ul>
     */
    @BuildStep
    void registerNettyThreadLocalCleanup(ActionBuilder action) {
        action
                .forService("io.quarkus.vertx.netty-thread-local-cleanup")
                .atPhase(Phase.STATIC_INIT)
                .before(IOThreadDetector.class)
                .before(ArcContainer.class)
                .action(ctx -> {
                    ctx.onStop(() -> {
                        Logger.getLogger("io.quarkus.vertx.netty-cleanup")
                                .info("Cleaning up Netty InternalThreadLocalMap");
                        io.netty.util.internal.InternalThreadLocalMap.remove();
                    });
                });
    }

    @BuildStep
    void configureLogging(ActionBuilder action) {
        action
                .forService("io.quarkus.vertx.configure-logging")
                .atPhase(Phase.LOGGING)
                .action(ctx -> VertxCoreRecorder.configureQuarkusLoggerFactory());
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    @Record(value = ExecutionTime.RUNTIME_INIT)
    CoreVertxBuildItem build(
            VertxCoreRecorder recorder,
            ActionBuilder action,
            LaunchModeBuildItem launchMode,
            ShutdownContextBuildItem shutdown,
            List<VertxBootstrapConsumerBuildItem> vertxBootstrapConsumers,
            List<VertxOptionsConsumerBuildItem> vertxOptionsConsumers,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<EventLoopSupplierBuildItem> eventLoops,
            ExecutorBuildItem executorBuildItem,
            MutinyRuntimeInitBuildItem barrier_mutinyRuntimeInitBuildItem) throws IOException, ClassNotFoundException {

        // Override the Mutiny infrastructure ScheduledExecutorService to dispatch scheduled operations to a Vert.x timer
        recorder.wrapMainExecutorForMutiny(executorBuildItem.getExecutorProxy());

        List<Consumer<VertxBootstrap>> bootstrapCustomizer = vertxBootstrapConsumers.stream()
                .sorted()
                .map(VertxBootstrapConsumerBuildItem::getConsumer)
                .toList();

        List<Consumer<VertxOptions>> optionsCustomizer = vertxOptionsConsumers.stream()
                .map(VertxOptionsConsumerBuildItem::getConsumer)
                .toList();

        // resolve the service class names at build time, they will be instantiated at runtime
        List<String> vertxServiceProviderClassNames = loadServiceClassNames(VertxServiceProvider.class);
        List<String> verticleFactoryClassNames = loadServiceClassNames(VerticleFactory.class);

        VertxSupplier vertx = recorder.configureVertx(launchMode.getLaunchMode(), shutdown, bootstrapCustomizer,
                optionsCustomizer,
                vertxServiceProviderClassNames, verticleFactoryClassNames, executorBuildItem.getExecutorProxy());
        action.aliasRecorderValue(VertxSupplier.class, vertx, Phase.INFRASTRUCTURE);
        action
                .forService(Vertx.class)
                .atPhase(Phase.INFRASTRUCTURE)
                .require(VertxSupplier.class)
                .action((ctx, supplier) -> supplier.get());
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(Vertx.class)
                .types(Vertx.class)
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .supplier(vertx).done());

        // Event loops are only usable after the core vertx instance is configured
        eventLoops.produce(new EventLoopSupplierBuildItem(recorder.mainSupplier(), recorder.bossSupplier()));

        if (launchMode.getLaunchMode().isDevOrTest()) {
            handleBlockingWarningsInDevOrTestMode();
        }
        return new CoreVertxBuildItem(vertx);
    }

    @BuildStep
    void detectNativeTransports(VertxBuildTimeConfig buildTimeConfig, ActionBuilder action) {
        Set<String> detected = new HashSet<>();

        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.channel.epoll.EpollMode")) {
            detected.add(NativeTransportType.EPOLL.transportName);
        }
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.channel.kqueue.AcceptFilter")) {
            detected.add(NativeTransportType.KQUEUE.transportName);
        }
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.channel.uring.IoUring")) {
            detected.add(NativeTransportType.IO_URING.transportName);
        }

        NativeTransportType requestedType = buildTimeConfig.nativeTransportType();
        NativeTransportMode mode = buildTimeConfig.nativeTransport();
        boolean preferNative = mode != NativeTransportMode.DISABLED || requestedType != NativeTransportType.AUTO;

        if (requestedType != NativeTransportType.AUTO && !detected.contains(requestedType.transportName)) {
            String msg = String.format(
                    "Native transport '%s' was requested (quarkus.vertx.native-transport-type=%s) "
                            + "but its dependency is not on the classpath. "
                            + "See the Native Transport Reference guide for the required dependency.",
                    requestedType.transportName, requestedType.name().toLowerCase().replace('_', '-'));
            if (mode == NativeTransportMode.REQUIRED) {
                throw new ConfigurationException(msg);
            }
            log.warn(msg);
        } else if (preferNative && detected.isEmpty()) {
            log.warn("Native transport was requested but no native transport dependency was found on the classpath. "
                    + "The application will fall back to Java NIO. "
                    + "See the Native Transport Reference guide for dependency information.");
        } else if (!detected.isEmpty()) {
            log.debugf("Detected native transport(s) on classpath: %s", detected);
        }

        Set<String> capturedDetected = Set.copyOf(detected);
        action
                .forService("io.quarkus.vertx.detect-native-transports")
                .atPhase(Phase.INFRASTRUCTURE)
                .action(ctx -> VertxCoreRecorder.setDetectedNativeTransports(capturedDetected));
    }

    @BuildStep
    LogCleanupFilterBuildItem filterNettyHostsFileParsingWarn() {
        return new LogCleanupFilterBuildItem("io.netty.resolver.HostsFileParser",
                "Failed to load and parse hosts file");
    }

    @BuildStep
    void registerVerticleClasses(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (ClassInfo ci : indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(AbstractVerticle.class.getName()))) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(ci.toString()).build());
        }
    }

    @BuildStep
    void doNotRemoveVertxOptionsCustomizers(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        unremovable.produce(UnremovableBeanBuildItem.beanTypes(VertxOptionsCustomizer.class));
    }

    @BuildStep
    ThreadFactoryBuildItem createVertxThreadFactory(ActionBuilder action, LaunchModeBuildItem launchMode) {
        LaunchMode mode = launchMode.getLaunchMode();
        action
                .forService(ThreadFactory.class)
                .atPhase(Phase.INFRASTRUCTURE)
                .action(ctx -> VertxCoreRecorder.createThreadFactory(mode));
        return new ThreadFactoryBuildItem(
                action.serviceAsRecorderValue(ThreadFactory.class));
    }

    @SuppressWarnings("unchecked")
    @BuildStep
    ContextHandlerBuildItem createVertxContextHandlers(ActionBuilder action,
            VertxBuildConfig buildConfig,
            List<IgnoredContextLocalDataKeysBuildItem> ignoredKeysSuppliersItems) {
        List<String> extensionIgnoredKeys = List.of(ignoredKeysSuppliersItems.stream()
                .flatMap(item -> item.ignoredKeys().stream())
                .toArray(String[]::new));
        boolean includeArcKeys = buildConfig.customizeArcContext();
        action
                .forService(ContextHandler.class, "io.quarkus.vertx.context-handler")
                .atPhase(Phase.INFRASTRUCTURE)
                .action(ctx -> VertxCoreRecorder.executionContextHandler(extensionIgnoredKeys, includeArcKeys));
        return new ContextHandlerBuildItem(
                (ContextHandler<Object>) action.serviceAsRecorderValue(
                        ContextHandler.class, "io.quarkus.vertx.context-handler"));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public void resetMapper(ActionBuilder action) {
        action
                .forService("io.quarkus.vertx.reset-mapper")
                .atPhase(Phase.INFRASTRUCTURE)
                .action(ctx -> ctx.onStop(QuarkusJacksonFactory::reset));
    }

    private void handleBlockingWarningsInDevOrTestMode() {
        try {
            Filter debuggerFilter = createDebuggerFilter();
            LogManager logManager = (LogManager) LogManager.getLogManager();
            Filter filter = new Filter() {

                volatile StackTraceElement last;

                @Override
                public boolean isLoggable(LogRecord record) {
                    if (debuggerFilter != null && !debuggerFilter.isLoggable(record)) {
                        return false;
                    }
                    //even if there is no debugger attached we might not want to log it
                    if (record.getThrown() == null) {
                        //no huge exception, so I guess this is ok
                        return true;
                    }
                    StackTraceElement element = record.getThrown().getStackTrace()[0];
                    if (last != null) {
                        //we don't want to just keep putting out the same warnings over and over
                        //it pollutes the logs and makes a big mess
                        if (element.equals(last)) {
                            return false;
                        }
                    }
                    last = element;
                    return true;
                }
            };

            for (String classname : BLOCKED_THREAD_LOGGER_NAMES) {
                logManager.getLogger(classname).setFilter(filter);
            }
        } catch (Throwable t) {
            log.debug("Failed to filter blocked thread checker", t);
        }
    }

    /**
     * Creates a filter that will filter out log messages if a debugger is attached, or null if
     * one can't be created (e.g. the JVM is not in debug mode).
     */
    private Filter createDebuggerFilter() {

        try {
            //we don't want breakpoints to trigger the vert.x blocked thread warning
            //no easy way to do this, so we do it the hard way
            var runtime = ManagementFactory.getRuntimeMXBean();
            if (runtime == null) {
                return null;
            }
            int debugPort = -1;
            InetAddress bindAddress = null;
            boolean alwaysFilter = false;
            var args = runtime.getInputArguments();
            for (var arg : args) {
                if (arg.startsWith("-Xrunjdwp") || arg.startsWith("-agentlib:jdwp")) {
                    boolean client = true;
                    if (!arg.contains("transport=dt_socket")) {
                        //we can only handle socket transport
                        return null;
                    }
                    Pattern server = Pattern.compile("server=(.)");
                    Matcher m = server.matcher(arg);
                    if (m.find()) {
                        if (m.group(1).equals("y")) {
                            client = false;
                        }
                    }
                    if (client) {
                        //for client mode we assume the debugger is always attached
                        //this is how IDE's run tests etc., so the debugger is attached right from the start
                        //in this mode we will never print the blocked thread warnings
                        alwaysFilter = true;
                        break;
                    }
                    Pattern port = Pattern.compile("address=(.*?):(\\d+)");
                    m = port.matcher(arg);
                    if (m.find()) {
                        debugPort = Integer.parseInt(m.group(2));
                        String host = m.group(1);
                        if (host.equals("*")) {
                            host = "localhost";
                        }
                        bindAddress = InetAddress.getByName(host);
                    }
                }
            }
            if (debugPort == -1 && !alwaysFilter) {
                return null;
            }

            Filter filter;
            LogManager logManager = (LogManager) LogManager.getLogManager();
            if (alwaysFilter) {
                filter = s -> false;
            } else {
                int port = debugPort;
                InetAddress bind = bindAddress;
                filter = new Filter() {
                    @Override
                    public boolean isLoggable(LogRecord record) {
                        try (ServerSocket s = new ServerSocket(port, 1, bind)) {

                        } catch (IOException e) {
                            //if we fail to bind the JVM is still waiting for a debugger to attach
                            //no debugger means log the warning
                            return true;
                        }
                        //if we get here we know the JVM is no longer binding the port
                        //which means a debugger has attached, and we disable the warning
                        return false;
                    }
                };
            }
            return filter;
        } catch (Throwable t) {
            log.debug("Failed to filter blocked thread checker", t);
            return null;
        }
    }

    @BuildStep
    void registerBlockingSecurityExecutor(BuildProducer<AdditionalBeanBuildItem> beanProducer,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.SECURITY) || capabilities.isPresent(Capability.OIDC_CLIENT)
                || capabilities.isPresent(Capability.OIDC_CLIENT_REGISTRATION)) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable()
                            .addBeanClass(io.quarkus.vertx.core.runtime.security.VertxBlockingSecurityExecutor.class)
                            .setDefaultScope(APPLICATION_SCOPED).build());
        }
    }

    private List<String> loadServiceClassNames(Class<?> serviceClass) throws IOException, ClassNotFoundException {
        List<String> serviceClassNames = new ArrayList<>();
        for (Class<?> serviceImplClass : ServiceUtil.classesNamedIn(Thread.currentThread().getContextClassLoader(),
                "META-INF/services/" + serviceClass.getName())) {
            if (!QuarkusClassLoader.isClassPresentAtRuntime(serviceImplClass.getName())) {
                continue;
            }
            serviceClassNames.add(serviceImplClass.getName());
        }
        return serviceClassNames;
    }
}
