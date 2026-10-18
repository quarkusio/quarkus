package io.quarkus.qute.trace;

import io.quarkus.qute.Engine;

/**
 * Base class for trace events related to template rendering.
 * <p>
 * Captures the engine instance and tracks execution duration.
 */
public abstract class BaseEvent {

    private final Engine engine;
    private final long startTime;
    private volatile long endTime = -1;

    /**
     * Creates a new event and starts the timer.
     *
     * @param engine the engine managing the rendering process
     */
    public BaseEvent(Engine engine) {
        this.engine = engine;
        this.startTime = System.nanoTime();
    }

    /**
     * Returns the engine managing the rendering process.
     *
     * @return the engine
     */
    public Engine getEngine() {
        return engine;
    }

    /**
     * Marks the event as completed and records the end time.
     */
    public void done() {
        endTime = System.nanoTime();
    }

    /**
     * Returns the elapsed time in nanoseconds between the start and end of the
     * event.
     * <p>
     * If the event is not marked as done, returns {@code -1}.
     *
     * @return elapsed time in nanoseconds, or -1 if not finished
     */
    public long getEllapsedTime() {
        if (endTime == -1) {
            return -1;
        }
        return endTime - startTime;
    }
}
