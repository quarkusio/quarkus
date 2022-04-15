package io.quarkus.test.junit.callback;

import java.util.List;

/**
 * Context object passed to {@link QuarkusTestAfterAllCallback}
 */
public class QuarkusTestContext {

    private final Object testInstance;
    private final List<Object> outerInstances;

    public QuarkusTestContext(Object testInstance, List<Object> outerInstances) {
        this.testInstance = testInstance;
        this.outerInstances = outerInstances;
    }

    public Object getTestInstance() {
        return testInstance;
    }

    public List<Object> getOuterInstances() {
        return outerInstances;
    }
}
