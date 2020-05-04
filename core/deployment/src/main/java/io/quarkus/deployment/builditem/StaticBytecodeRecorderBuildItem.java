package io.quarkus.deployment.builditem;

import io.quarkus.builder.StepDependencyInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;

public final class StaticBytecodeRecorderBuildItem extends MultiBuildItem implements RecordedBytecode {

    private final BytecodeRecorderImpl bytecodeRecorder;
    private final StepDependencyInfo stepDependencyInfo;

    public StaticBytecodeRecorderBuildItem(BytecodeRecorderImpl bytecodeRecorder, StepDependencyInfo stepDependencyInfo) {
        this.bytecodeRecorder = bytecodeRecorder;
        this.stepDependencyInfo = stepDependencyInfo;
    }

    @Override
    public BytecodeRecorderImpl getBytecodeRecorder() {
        return bytecodeRecorder;
    }

    @Override
    public StepDependencyInfo getStepDependencyInfo() {
        return stepDependencyInfo;
    }
}
