package io.quarkus.it.main;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class TestContextCheckerAfterEachCallback implements QuarkusTestAfterEachCallback {

    public static final List<Object> OUTER_INSTANCES = new ArrayList<>();

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        OUTER_INSTANCES.clear();
        OUTER_INSTANCES.addAll(context.getOuterInstances());
    }
}
