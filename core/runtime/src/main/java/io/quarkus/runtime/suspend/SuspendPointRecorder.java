package io.quarkus.runtime.suspend;

import io.quarkus.runtime.annotations.Recorder;

/**
 * A recorder proxy for {@link SuspendPoint} and related utilities.
 */
@Recorder
public final class SuspendPointRecorder {
    public SuspendPointRecorder() {}

    public void readyToSuspend() {
        SuspendPoint.readyToSuspend();
    }

    public void readyForRequests() {
        SuspendPoint.readyForRequests();
    }

    public void initialize(String className) {
        try {
            Class.forName(className, true, SuspendPointRecorder.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
        }
    }
}
