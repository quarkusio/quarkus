package io.quarkus.virtual.threads;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;

public class VirtualThreadsProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void setup(VirtualThreadsConfig config, VirtualThreadsRecorder recorder,
            ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        recorder.setupVirtualThreads(config, shutdownContextBuildItem, launchModeBuildItem.getLaunchMode());
    }

}
