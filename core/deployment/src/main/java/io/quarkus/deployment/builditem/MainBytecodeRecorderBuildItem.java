package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;

public final class MainBytecodeRecorderBuildItem extends MultiBuildItem {

    private final BytecodeRecorderImpl bytecodeRecorder;
    private final String generatedStartupContextClassName;

    public MainBytecodeRecorderBuildItem(BytecodeRecorderImpl bytecodeRecorder) {
        this.bytecodeRecorder = bytecodeRecorder;
        this.generatedStartupContextClassName = null;
    }

    public MainBytecodeRecorderBuildItem(String generatedStartupContextClassName) {
        this.generatedStartupContextClassName = generatedStartupContextClassName;
        this.bytecodeRecorder = null;
    }

    public BytecodeRecorderImpl getBytecodeRecorder() {
        return bytecodeRecorder;
    }

    public String getGeneratedStartupContextClassName() {
        return generatedStartupContextClassName;
    }
}
