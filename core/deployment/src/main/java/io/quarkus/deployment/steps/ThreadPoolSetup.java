package io.quarkus.deployment.steps;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.ThreadPoolConfig;

/**
 *
 */
public class ThreadPoolSetup {

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
    public ExecutorBuildItem createExecutor(ExecutorRecorder recorder, ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            ThreadPoolConfig threadPoolConfig) {
        return new ExecutorBuildItem(
                recorder.setupRunTime(shutdownContextBuildItem, threadPoolConfig, launchModeBuildItem.getLaunchMode()));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem registerClasses() {
        // make sure that the config provider gets initialized only at run time
        return new RuntimeInitializedClassBuildItem(ExecutorRecorder.class.getName());
    }
}
