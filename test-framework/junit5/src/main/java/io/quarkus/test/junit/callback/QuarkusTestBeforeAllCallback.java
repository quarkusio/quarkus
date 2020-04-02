package io.quarkus.test.junit.callback;

public interface QuarkusTestBeforeAllCallback {

    void beforeAll(Object testInstance);
}
