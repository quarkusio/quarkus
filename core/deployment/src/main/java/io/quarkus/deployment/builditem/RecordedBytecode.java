package io.quarkus.deployment.builditem;

import io.quarkus.builder.StepDependencyInfo;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;

public interface RecordedBytecode {
    BytecodeRecorderImpl getBytecodeRecorder();

    StepDependencyInfo getStepDependencyInfo();
}
