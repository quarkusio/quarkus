package io.quarkus.arc.deployment;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExecutorBuildItem;

public class ExecutorServiceProcessor {

    @BuildStep
    SyntheticBeanBuildItem executorServiceBean(ExecutorBuildItem executor) {
        return SyntheticBeanBuildItem.configure(ScheduledExecutorService.class)
                .types(ExecutorService.class, Executor.class).scope(BuiltinScope.APPLICATION.getInfo()).setRuntimeInit()
                .runtimeProxy(executor.getExecutorProxy()).done();
    }

}
