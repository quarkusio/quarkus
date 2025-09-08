package io.quarkus.deployment.steps;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.runtime.JVMChecksRecorder;

public class CheckJVMparamsProcessor {

    @BuildStep(onlyIfNot = NativeBuild.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void recordJvmChecks(JVMChecksRecorder recorder) {
        recorder.check();
    }
}
