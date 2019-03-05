package io.quarkus.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;

import io.quarkus.deployment.recording.BytecodeRecorderImpl;

public final class MainAfterStartupBytecodeRecorderBuildItem extends MultiBuildItem {

    private final BytecodeRecorderImpl bytecodeRecorder;

    public MainAfterStartupBytecodeRecorderBuildItem(BytecodeRecorderImpl bytecodeRecorder) {
        this.bytecodeRecorder = bytecodeRecorder;
    }

    public BytecodeRecorderImpl getBytecodeRecorder() {
        return bytecodeRecorder;
    }
}
