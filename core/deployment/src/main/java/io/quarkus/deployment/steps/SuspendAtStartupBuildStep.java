package io.quarkus.deployment.steps;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationStartBuildItem;
import io.quarkus.deployment.builditem.PostResumeBuildItem;
import io.quarkus.deployment.builditem.PreSuspendBuildItem;
import io.quarkus.runtime.suspend.SuspendPointRecorder;

/**
 * Build steps which relate to suspend-at-startup capabilities.
 */
public class SuspendAtStartupBuildStep {
    /**
     * Implement the suspend-resume step.
     *
     * @param recorder the suspend point recorder
     */
    @BuildStep
    @Consume(PreSuspendBuildItem.class)
    @Produce(PostResumeBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void suspendResumeStep(SuspendPointRecorder recorder) {
        recorder.readyToSuspend();
    }

    /**
     * Unblock the resume thread when we're ready for requests.
     *
     * @param recorder the suspend point recorder
     */
    @Consume(PostResumeBuildItem.class)
    @Consume(ApplicationStartBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void resumeCompleteStep(SuspendPointRecorder recorder) {
        recorder.readyForRequests();
    }
}
