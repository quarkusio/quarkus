package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;

/**
 * This build item will be used to write bytecode that supports the Bootstrap phase of the configuration
 * That code essentially uses part of the configuration system to pass configuration data to
 * recorders that then use the configuration to create new configuration sources.
 * These sources are then used to create the final runtime configuration which then passed on
 * to all the other runtime recorders
 */
public final class MainBootstrapConfigBytecodeRecorderBuildItem extends MultiBuildItem {

    private final BytecodeRecorderImpl bytecodeRecorder;

    public MainBootstrapConfigBytecodeRecorderBuildItem(BytecodeRecorderImpl bytecodeRecorder) {
        this.bytecodeRecorder = bytecodeRecorder;
    }

    public BytecodeRecorderImpl getBytecodeRecorder() {
        return bytecodeRecorder;
    }
}
