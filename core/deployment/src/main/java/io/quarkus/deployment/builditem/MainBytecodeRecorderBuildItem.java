package io.quarkus.deployment.builditem;

import io.quarkus.builder.StepDependencyInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;

public final class MainBytecodeRecorderBuildItem extends MultiBuildItem implements RecordedBytecode {

    private final BytecodeRecorderImpl bytecodeRecorder;
    private final StepDependencyInfo stepDependencyInfo;

    public MainBytecodeRecorderBuildItem(BytecodeRecorderImpl bytecodeRecorder, StepDependencyInfo stepDependencyInfo) {
        this.bytecodeRecorder = bytecodeRecorder;
        this.stepDependencyInfo = stepDependencyInfo;
    }

    public MainBytecodeRecorderBuildItem(String generatedStartupTaskClassName, StepDependencyInfo stepDependencyInfo) {
        this.stepDependencyInfo = stepDependencyInfo;
        this.bytecodeRecorder = null;
    }

    public BytecodeRecorderImpl getBytecodeRecorder() {
        return bytecodeRecorder;
    }

    public StepDependencyInfo getStepDependencyInfo() {
        return stepDependencyInfo;
    }
}
