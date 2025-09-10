package io.quarkus.deployment.steps;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.runtime.JVMChecksRecorder;

public class JVMUnsafeWarningsProcessor {

    @BuildStep(onlyIfNot = NativeBuild.class)
    @Record(ExecutionTime.STATIC_INIT) //We need this to trigger before Netty or other Unsafe users get to run: static init seems effective enough
    public void disableUnsafeRelatedWarnings(JVMChecksRecorder recorder) {
        recorder.disableUnsafeRelatedWarnings();
    }

}
