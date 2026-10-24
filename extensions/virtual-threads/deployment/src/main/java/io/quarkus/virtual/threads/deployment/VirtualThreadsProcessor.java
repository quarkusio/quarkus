package io.quarkus.virtual.threads.deployment;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.virtual.threads.VirtualThreads;
import io.quarkus.virtual.threads.VirtualThreadsConfig;
import io.quarkus.virtual.threads.VirtualThreadsExecutorCreator;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;

public class VirtualThreadsProcessor {

    @BuildStep
    public void setup(ServiceRegistrar serviceRegistrar,
            VirtualThreadsConfig config,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<SyntheticBeanBuildItem> producer) {
        beans.produce(new AdditionalBeanBuildItem(VirtualThreads.class));
        LaunchMode launchMode = launchModeBuildItem.getLaunchMode();
        serviceRegistrar
                .forService("io.quarkus.virtual-threads.executor")
                .atPhase(Phase.STATIC_INIT)
                .before(ArcContainer.class)
                .onStart(ctx -> {
                    VirtualThreadsRecorder.setConfig(config);
                    if (config.enabled()) {
                        ctx.onStop(() -> VirtualThreadsRecorder.shutdown(config, launchMode));
                    }
                });
        producer.produce(
                SyntheticBeanBuildItem.configure(ExecutorService.class)
                        .addType(Executor.class)
                        .addQualifier(AnnotationInstance.builder(VirtualThreads.class).build())
                        .scope(BuiltinScope.APPLICATION.getInfo())
                        .setRuntimeInit()
                        .creator(VirtualThreadsExecutorCreator.class)
                        .done());
    }
}
