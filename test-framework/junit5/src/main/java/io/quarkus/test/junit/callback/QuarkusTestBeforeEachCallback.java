package io.quarkus.test.junit.callback;

public interface QuarkusTestBeforeEachCallback {

    void beforeEach(Object testInstance);
}
