package io.quarkus.vertx.common.deployment;

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
import io.quarkus.vertx.common.runtime.VertxCommonProducer;
import io.quarkus.vertx.common.runtime.VertxCommonRecorder;
import io.quarkus.vertx.common.runtime.VertxConfiguration;
import io.vertx.core.Vertx;

class VertxCommonProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    SubstrateConfigBuildItem build() {
        return SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("vertx.disableDnsResolver", "true")
                .addRuntimeInitializedClass("io.vertx.core.http.impl.VertxHttp2ClientUpgradeCodec")
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(VertxCommonProducer.class);
    }

    @Record(ExecutionTime.STATIC_INIT)
    EventLoopSupplierBuildItem eventLoop(VertxCommonRecorder recorder) {
        return new EventLoopSupplierBuildItem(recorder.mainSupplier(), recorder.bossSupplier());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    IOThreadDetectorBuildItem ioThreadDetector(VertxCommonRecorder recorder) {
        return new IOThreadDetectorBuildItem(recorder.detector());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    InternalVertxBuildItem build(VertxCommonRecorder recorder, BeanContainerBuildItem beanContainer,
            LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown, VertxConfiguration config) {

        RuntimeValue<Vertx> vertx = recorder.configureVertx(beanContainer.getValue(), config,
                launchMode.getLaunchMode(), shutdown);

        return new InternalVertxBuildItem(vertx);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    InternalWebVertxBuildItem buildWeb(VertxCommonRecorder recorder, VertxConfiguration config,
            ShutdownContextBuildItem context,
            LaunchModeBuildItem launchModeBuildItem) {
        RuntimeValue<Vertx> vertx = recorder.initializeWeb(config, context, launchModeBuildItem.getLaunchMode());
        return new InternalWebVertxBuildItem(vertx);
    }
}
