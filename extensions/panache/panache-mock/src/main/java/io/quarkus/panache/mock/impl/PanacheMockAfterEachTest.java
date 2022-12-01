package io.quarkus.panache.mock.impl;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class PanacheMockAfterEachTest implements QuarkusTestAfterEachCallback {

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        PanacheMock.reset();
    }

}
