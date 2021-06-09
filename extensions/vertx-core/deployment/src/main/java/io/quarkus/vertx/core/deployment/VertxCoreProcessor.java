package io.quarkus.vertx.core.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

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
import io.vertx.core.spi.resolver.ResolverProvider;

class VertxCoreProcessor {

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
    ThreadFactoryBuildItem createVertxThreadFactory(VertxCoreRecorder recorder) {
        return new ThreadFactoryBuildItem(recorder.createThreadFactory());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ContextHandlerBuildItem createVertxContextHandlers(VertxCoreRecorder recorder) {
        return new ContextHandlerBuildItem(recorder.executionContextHandler());
    }
}
