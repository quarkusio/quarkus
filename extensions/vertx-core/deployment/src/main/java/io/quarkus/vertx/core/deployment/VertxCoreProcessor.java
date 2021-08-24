package io.quarkus.vertx.core.deployment;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogManager;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
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
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.netty.deployment.EventLoopSupplierBuildItem;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.VertxLogDelegateFactory;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.BlockedThreadChecker;
import io.vertx.core.spi.resolver.ResolverProvider;

class VertxCoreProcessor {

    private static final Logger log = Logger.getLogger(VertxCoreProcessor.class);

    @BuildStep
    NativeImageConfigBuildItem build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, VertxLogDelegateFactory.class.getName()));
        return NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("io.vertx.core.net.impl.PartialPooledByteBufAllocator")
                .addRuntimeInitializedClass("io.vertx.core.http.impl.VertxHttp2ClientUpgradeCodec")
                .addRuntimeInitializedClass("io.vertx.core.eventbus.impl.clustered.ClusteredEventBus")

                .addNativeImageSystemProperty(ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME, "true")
                .addNativeImageSystemProperty("vertx.logger-delegate-factory-class-name",
                        VertxLogDelegateFactory.class.getName())
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    EventLoopCountBuildItem eventLoopCount(VertxCoreRecorder recorder, VertxConfiguration vertxConfiguration) {
        return new EventLoopCountBuildItem(recorder.calculateEventLoopThreads(vertxConfiguration));
    }

    @BuildStep
    LogCleanupFilterBuildItem cleanupVertxWarnings() {
        return new LogCleanupFilterBuildItem("io.vertx.core.impl.ContextImpl", "You have disabled TCCL checks");
    }

    @BuildStep
    LogCategoryBuildItem preventLoggerContention() {
        //Prevent the Logging warning about the TCCL checks being disabled to be logged;
        //this is similar to #cleanupVertxWarnings but prevents it by changing the level:
        // it takes advantage of the fact that there is a single other log in thi class,
        // and it happens to be at error level.
        //This is more effective than the LogCleanupFilterBuildItem as we otherwise have
        //contention since this message could be logged very frequently.
        return new LogCategoryBuildItem("io.vertx.core.impl.ContextImpl", Level.ERROR);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    IOThreadDetectorBuildItem ioThreadDetector(VertxCoreRecorder recorder) {
        return new IOThreadDetectorBuildItem(recorder.detector());
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    @Record(value = ExecutionTime.RUNTIME_INIT)
    CoreVertxBuildItem build(VertxCoreRecorder recorder,
            LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown, VertxConfiguration config,
            List<VertxOptionsConsumerBuildItem> vertxOptionsConsumers,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<EventLoopSupplierBuildItem> eventLoops,
            ExecutorBuildItem executorBuildItem) {

        Collections.sort(vertxOptionsConsumers);
        List<Consumer<VertxOptions>> consumers = new ArrayList<>(vertxOptionsConsumers.size());
        for (VertxOptionsConsumerBuildItem x : vertxOptionsConsumers) {
            consumers.add(x.getConsumer());
        }

        Supplier<Vertx> vertx = recorder.configureVertx(config,
                launchMode.getLaunchMode(), shutdown, consumers, executorBuildItem.getExecutorProxy());
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
    LogCleanupFilterBuildItem filterNettyHostsFileParsingWarn() {
        return new LogCleanupFilterBuildItem("io.netty.resolver.HostsFileParser",
                "Failed to load and parse hosts file");
    }

    @BuildStep
    void registerVerticleClasses(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (ClassInfo ci : indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(AbstractVerticle.class.getName()))) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ci.toString()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ThreadFactoryBuildItem createVertxThreadFactory(VertxCoreRecorder recorder, LaunchModeBuildItem launchMode) {
        return new ThreadFactoryBuildItem(recorder.createThreadFactory(launchMode.getLaunchMode()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ContextHandlerBuildItem createVertxContextHandlers(VertxCoreRecorder recorder) {
        return new ContextHandlerBuildItem(recorder.executionContextHandler());
    }

    private void handleBlockingWarningsInDevOrTestMode() {
        try {
            Filter debuggerFilter = createDebuggerFilter();
            LogManager logManager = (LogManager) LogManager.getLogManager();
            logManager.getLogger(BlockedThreadChecker.class.getName()).setFilter(new Filter() {

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
            });
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
                        //this is how IDE's run tests etc, so the debugger is attached right from the start
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
                            host.equals("localhost");
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
}
