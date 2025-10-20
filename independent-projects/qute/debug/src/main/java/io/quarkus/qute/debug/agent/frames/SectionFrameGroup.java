package io.quarkus.qute.debug.agent.frames;

import java.util.LinkedList;

import io.quarkus.qute.LoopSectionHelper;
import io.quarkus.qute.Mapper;
import io.quarkus.qute.SectionHelper;

/**
 * Represents a group of {@link RemoteStackFrame}s belonging to a Qute section
 * (e.g. {@code #for}, {@code #if}, {@code #each}).
 * <p>
 * Each group tracks frames created within a single section instance and manages
 * their lifecycle during template rendering:
 * <ul>
 * <li>When the section ends — all frames are removed.</li>
 * <li>When a {@code #for} or {@code #each} loop advances to another iteration —
 * the frames of the previous iteration are replaced.</li>
 * </ul>
 * </p>
 */
public class SectionFrameGroup {

    private static final String ITERATION_ELEMENT_CLASS_NAME = "IterationElement"; // LoopSectionHelper$IterationElement

    private final LinkedList<RemoteStackFrame> frames = new LinkedList<>();
    private final String metadataIndex;
    private int index;

    public SectionFrameGroup(SectionHelper sectionHelper) {
        this.metadataIndex = sectionHelper instanceof LoopSectionHelper loopSectionHelper
                ? loopSectionHelper.getMetadataPrefix() + "index"
                : null;
    }

    /**
     * Returns the current iteration index (for {@code #for}/{@code #each}
     * sections).
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets a new iteration index and clears existing frames. This is typically
     * called when a {@code #for} or {@code #each} section moves to the next
     * iteration.
     */
    private void setIndex(int newIndex) {
        this.index = newIndex;
        this.frames.clear();
    }

    /**
     * Adds a frame to this section group.
     */
    public void addFrame(RemoteStackFrame frame) {
        frames.addFirst(frame);
    }

    /**
     * Removes all frames of this section from the given thread frame list.
     * <p>
     * Used both when:
     * <ul>
     * <li>The section ends (e.g. {@code #endfor}, {@code #endif}).</li>
     * <li>A {@code #for} or {@code #each} section advances to another
     * iteration.</li>
     * </ul>
     * </p>
     */
    public void detachFrames(LinkedList<RemoteStackFrame> threadFrames) {
        threadFrames.removeAll(frames);
    }

    /**
     * Checks if the current iteration index has changed, and detaches the frames
     * from the given thread frame list if needed.
     *
     * This is used for loop sections (#for, #each) in Qute. When the loop advances
     * to a new iteration, the frames associated with the previous iteration must be
     * removed from the thread's active stack.
     *
     * @param data the loop iteration element (expected class:
     *        LoopSectionHelper$IterationElement)
     * @param threadFrames the list of frames in the RemoteThread
     */
    public void detachFramesIfIndexChanged(Object data, LinkedList<RemoteStackFrame> threadFrames) {
        if (metadataIndex != null && data instanceof Mapper mapper
                && ITERATION_ELEMENT_CLASS_NAME.equals(data.getClass().getSimpleName())) {
            // It is an LoopSectionHelper$IterationElement
            try {
                // Get the current index of LoopSectionHelper$IterationElement
                var result = mapper.getAsync(metadataIndex).toCompletableFuture().getNow(null);
                if (result instanceof Integer currentIndex) {
                    // LoopSectionHelper$IterationElement#index has been collected
                    if (currentIndex != this.index) {
                        // index changed, remove frames from the current iteration
                        detachFrames(threadFrames);
                        // initialize with the new index
                        setIndex(currentIndex);
                    }
                }
            } catch (Exception e) {
                // ignore error
            }

        }
    }

}
