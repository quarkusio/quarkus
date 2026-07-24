package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.core.deployment.action.impl.TransliteratedAction;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;

/**
 * A build item holding bytecode recording information relevant to the static initializer.
 * <p>
 * Instances of this item can hold either:
 * <ul>
 * <li>A direct {@link BytecodeRecorderImpl} instance via {@link #getBytecodeRecorder()}.</li>
 * <li>A {@link TransliteratedAction} for deferred consolidated class generation.</li>
 * </ul>
 *
 * @deprecated Extension authors should use {@link ActionBuilderImpl} instead of producing
 *             this item directly. This item is still used internally by the framework to
 *             schedule static-init startup tasks.
 */
//@Deprecated(since = "4.0")
public final class StaticBytecodeRecorderBuildItem extends MultiBuildItem {

    private final BytecodeRecorderImpl bytecodeRecorder;
    private final TransliteratedAction transliteratedAction;
    private final String stepId;

    /**
     * Construct a new instance from a bytecode recorder.
     *
     * @param bytecodeRecorder the bytecode recorder (must not be {@code null})
     * @param stepId the producing build step's ID (must not be {@code null})
     */
    public StaticBytecodeRecorderBuildItem(BytecodeRecorderImpl bytecodeRecorder, String stepId) {
        this.bytecodeRecorder = bytecodeRecorder;
        this.transliteratedAction = null;
        this.stepId = stepId;
    }

    /**
     * Construct a new instance from a transliterated action for deferred class generation.
     *
     * @param transliteratedAction the transliterated action data (must not be {@code null})
     */
    public StaticBytecodeRecorderBuildItem(TransliteratedAction transliteratedAction) {
        this.transliteratedAction = transliteratedAction;
        this.bytecodeRecorder = null;
        this.stepId = transliteratedAction.stepId();
    }

    /**
     * Get the bytecode recorder, if present.
     *
     * @return the bytecode recorder, or {@code null} if this item was constructed with a transliterated action
     */
    public BytecodeRecorderImpl getBytecodeRecorder() {
        return bytecodeRecorder;
    }

    /**
     * Get the transliterated action data, if present.
     *
     * @return the transliterated action, or {@code null} if this item was constructed with a bytecode recorder
     */
    public TransliteratedAction getTransliteratedAction() {
        return transliteratedAction;
    }

    /**
     * Get the ID of the build step that produced this item.
     *
     * @return the step ID (never {@code null})
     */
    public String getStepId() {
        return stepId;
    }
}
