package io.quarkus.vertx.core.deployment;

import javax.inject.Inject;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.IOThreadDetectorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.netty.deployment.EventLoopSupplierBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.core.runtime.VertxConfiguration;
import io.quarkus.vertx.core.runtime.VertxCoreProducer;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Vertx;

class VertxCommonProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    SubstrateConfigBuildItem build() {
        return SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("vertx.disableDnsResolver", "true")
                .addRuntimeInitializedClass("io.vertx.core.http.impl.VertxHttp2ClientUpgradeCodec")
                .addRuntimeReinitializedClass("io.vertx.core.net.impl.PartialPooledByteBufAllocator")
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(VertxCoreProducer.class);
    }

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
    @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
    CoreVertxBuildItem build(VertxCoreRecorder recorder, BeanContainerBuildItem beanContainer,
            LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown, VertxConfiguration config) {

        RuntimeValue<Vertx> vertx = recorder.configureVertx(beanContainer.getValue(), config,
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
}
