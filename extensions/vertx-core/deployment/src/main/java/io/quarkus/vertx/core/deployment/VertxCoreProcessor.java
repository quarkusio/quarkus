package io.quarkus.vertx.core.deployment;

import java.util.function.Supplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.IOThreadDetectorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.netty.deployment.EventLoopSupplierBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.core.runtime.VertxCoreProducer;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.VertxLogDelegateFactory;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.Vertx;

class VertxCoreProcessor {

    @BuildStep
    NativeImageConfigBuildItem build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, VertxLogDelegateFactory.class.getName()));
        return NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("io.vertx.core.net.impl.PartialPooledByteBufAllocator")
                .addRuntimeInitializedClass("io.vertx.core.http.impl.VertxHttp2ClientUpgradeCodec")
                .addRuntimeInitializedClass("io.vertx.core.eventbus.impl.clustered.ClusteredEventBus")

                .addNativeImageSystemProperty("vertx.disableDnsResolver", "true")
                .addNativeImageSystemProperty("vertx.logger-delegate-factory-class-name",
                        VertxLogDelegateFactory.class.getName())
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(VertxCoreProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    EventLoopCountBuildItem eventLoopCount(VertxCoreRecorder recorder, VertxConfiguration vertxConfiguration) {
        return new EventLoopCountBuildItem(recorder.calculateEventLoopThreads(vertxConfiguration));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    EventLoopSupplierBuildItem eventLoop(VertxCoreRecorder recorder) {
        return new EventLoopSupplierBuildItem(recorder.mainSupplier(), recorder.bossSupplier());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    IOThreadDetectorBuildItem ioThreadDetector(VertxCoreRecorder recorder) {
        return new IOThreadDetectorBuildItem(recorder.detector());
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    CoreVertxBuildItem build(VertxCoreRecorder recorder, BeanContainerBuildItem beanContainer,
            LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown, VertxConfiguration config) {

        Supplier<Vertx> vertx = recorder.configureVertx(beanContainer.getValue(), config,
                launchMode.getLaunchMode(), shutdown);

        return new CoreVertxBuildItem(vertx);
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
    InternalWebVertxBuildItem buildWeb(VertxCoreRecorder recorder, VertxConfiguration config,
            ShutdownContextBuildItem context,
            LaunchModeBuildItem launchModeBuildItem) {
        RuntimeValue<Vertx> vertx = recorder.initializeWeb(config, context, launchModeBuildItem.getLaunchMode());
        return new InternalWebVertxBuildItem(vertx);
    }

    @BuildStep
    LogCleanupFilterBuildItem filterNettyHostsFileParsingWarn() {
        return new LogCleanupFilterBuildItem("io.netty.resolver.HostsFileParser", "Failed to load and parse hosts file");
    }
}
