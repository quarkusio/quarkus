package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.shamrock.deployment.recording.BytecodeRecorderImpl;

public final class StaticBytecodeRecorderBuildItem extends MultiBuildItem {

    private final BytecodeRecorderImpl bytecodeRecorder;

    public StaticBytecodeRecorderBuildItem(BytecodeRecorderImpl bytecodeRecorder) {
        this.bytecodeRecorder = bytecodeRecorder;
    }

    public BytecodeRecorderImpl getBytecodeRecorder() {
        return bytecodeRecorder;
    }
}
