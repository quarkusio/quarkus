package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;

/**
 * A build item holding bytecode recording information relevant to the main application startup.
 * <p>
 * Instances of this item can hold either:
 * <ul>
 * <li>A direct {@link io.quarkus.deployment.recording.BytecodeRecorderImpl} instance via {@link #bytecodeRecorder}.</li>
 * <li>The name of a generated startup context class via {@link #generatedStartupContextClassName}.</li>
 * </ul>
 */
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
