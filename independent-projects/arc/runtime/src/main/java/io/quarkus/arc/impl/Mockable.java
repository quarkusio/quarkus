package io.quarkus.arc.impl;

/**
 * An interface implemented by mockable components when running in test mode.
 * <p>
 * This allows normal scoped beans to be easily mocked for tests.
 */
public interface Mockable {

    void arc$setMock(Object instance);

    void arc$clearMock();
}
