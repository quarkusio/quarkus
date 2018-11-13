package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.shamrock.deployment.recording.BytecodeRecorder;

/**
 * TODO: this class is wrong, at the moment it basically just provides a way to make sure all bytecode is generated
 * it needs to be re-thought out
 */
public final class RecordedBytecodeBuildItem extends MultiBuildItem {

    private final BytecodeRecorder recorder;

    public RecordedBytecodeBuildItem(BytecodeRecorder recorder) {
        this.recorder = recorder;
    }
}
