package io.quarkus.deployment.dev.io;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.dev.testing.TestSetupBuildItem;
import io.quarkus.runtime.dev.io.NioThreadPoolRecorder;

public class NioThreadPoolDevModeProcessor {

    @Produce(TestSetupBuildItem.class)
    @BuildStep(onlyIfNot = IsNormal.class)
    @Record(ExecutionTime.STATIC_INIT)
    void setupTCCL(NioThreadPoolRecorder recorder, ShutdownContextBuildItem shutdownContextBuildItem) {
        recorder.updateTccl(shutdownContextBuildItem);
    }

}
