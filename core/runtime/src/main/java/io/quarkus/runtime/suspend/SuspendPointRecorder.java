package io.quarkus.runtime.suspend;

import io.quarkus.runtime.annotations.Recorder;

/**
 * A recorder proxy for {@link SuspendPoint} and related utilities.
 */
@Recorder
public final class SuspendPointRecorder {
    public SuspendPointRecorder() {}

    public void startupComplete() {
        SuspendPoint.startupComplete();
    }

    public void initialize(String className) {
        try {
            Class.forName(className, true, SuspendPointRecorder.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
        }
    }
}
