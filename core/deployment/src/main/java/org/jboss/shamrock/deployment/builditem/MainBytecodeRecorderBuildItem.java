package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.shamrock.deployment.recording.BytecodeRecorderImpl;

public final class MainBytecodeRecorderBuildItem extends MultiBuildItem {

    private final BytecodeRecorderImpl bytecodeRecorder;

    public MainBytecodeRecorderBuildItem(BytecodeRecorderImpl bytecodeRecorder) {
        this.bytecodeRecorder = bytecodeRecorder;
    }

    public BytecodeRecorderImpl getBytecodeRecorder() {
        return bytecodeRecorder;
    }


}
