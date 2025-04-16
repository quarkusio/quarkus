package io.quarkus.runtime;

/**
 * Usually, QuarkusMock mocking replaces a "delegating instance" of a client proxy.
 * <p>
 * In some cases, e.g. for REST Client, a CDI bean is a wrapper over a delegate.
 * This interface allows to replace the delegate instead of the delegating instance of the proxy.
 */
public interface MockedThroughWrapper {
    void setMock(Object mock);

    void clearMock();
}
