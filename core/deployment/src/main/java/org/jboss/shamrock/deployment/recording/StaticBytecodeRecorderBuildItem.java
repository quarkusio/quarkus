package org.jboss.shamrock.deployment.recording;

import org.jboss.builder.item.MultiBuildItem;

public final class StaticBytecodeRecorderBuildItem extends MultiBuildItem {

    private final BytecodeRecorderImpl bytecodeRecorder;

    public StaticBytecodeRecorderBuildItem(BytecodeRecorderImpl bytecodeRecorder) {
        this.bytecodeRecorder = bytecodeRecorder;
    }

    public BytecodeRecorderImpl getBytecodeRecorder() {
        return bytecodeRecorder;
    }
}
