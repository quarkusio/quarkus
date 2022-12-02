package io.quarkus.it.main;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;

public class TestContextCheckerAfterAllCallback implements QuarkusTestAfterAllCallback {

    public static final List<Object> OUTER_INSTANCES = new ArrayList<>();

    @Override
    public void afterAll(QuarkusTestContext context) {
        OUTER_INSTANCES.clear();
        OUTER_INSTANCES.addAll(context.getOuterInstances());
    }
}
