package io.quarkus.panache.mock.impl;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;

public class PanacheMockAfterEachTest implements QuarkusTestAfterEachCallback {

    @Override
    public void afterEach(Object testInstance) {
        PanacheMock.reset();
    }

}
