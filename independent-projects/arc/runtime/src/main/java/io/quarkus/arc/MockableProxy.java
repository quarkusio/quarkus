package io.quarkus.arc;

/**
 * An interface that client proxies will implement when running in test mode.
 *
 * This allows normal scoped beans to be easily mocked for tests.
 */
public interface MockableProxy {

    void quarkus$$setMock(Object instance);

    void quarkus$$clearMock();
}
