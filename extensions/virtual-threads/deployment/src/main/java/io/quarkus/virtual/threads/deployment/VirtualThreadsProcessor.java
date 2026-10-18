package io.quarkus.virtual.threads.deployment;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.virtual.threads.VirtualThreads;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;

public class VirtualThreadsProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void setup(VirtualThreadsRecorder recorder,
            ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<SyntheticBeanBuildItem> producer) {
        beans.produce(new AdditionalBeanBuildItem(VirtualThreads.class));
        recorder.setupVirtualThreads(shutdownContextBuildItem, launchModeBuildItem.getLaunchMode());
        producer.produce(
                SyntheticBeanBuildItem.configure(ExecutorService.class)
                        .addType(Executor.class)
                        .addQualifier(AnnotationInstance.builder(VirtualThreads.class).build())
                        .scope(BuiltinScope.APPLICATION.getInfo())
                        .setRuntimeInit()
                        .supplier(recorder.getCurrentSupplier())
                        .done());
    }
}
