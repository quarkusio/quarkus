package io.quarkus.test.junit.internal;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;

/**
 * Strategy to deep clone an object
 * <p>
 * Used in order to clone an object loaded from one ClassLoader into another
 */
public interface DeepClone {

    Object clone(Object objectToClone);

    void setRunningQuarkusApplication(RunningQuarkusApplication runningQuarkusApplication);
}
