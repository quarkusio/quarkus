package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;

public final class BytecodeRecorderBuildItem extends MultiBuildItem {

    private final BytecodeRecorderImpl bytecodeRecorder;
    private final String generatedStartupContextClassName;
    private final ExecutionTime executionTime;

    public BytecodeRecorderBuildItem(BytecodeRecorderImpl bytecodeRecorder, ExecutionTime executionTime) {
        this.bytecodeRecorder = bytecodeRecorder;
        this.executionTime = executionTime;
        this.generatedStartupContextClassName = null;
    }

    public BytecodeRecorderBuildItem(String generatedStartupContextClassName, ExecutionTime executionTime) {
        this.generatedStartupContextClassName = generatedStartupContextClassName;
        this.executionTime = executionTime;
        this.bytecodeRecorder = null;
    }

    public BytecodeRecorderImpl getBytecodeRecorder() {
        return bytecodeRecorder;
    }

    public String getGeneratedStartupContextClassName() {
        return generatedStartupContextClassName;
    }

    public ExecutionTime getExecutionTime() {
        return executionTime;
    }
}
