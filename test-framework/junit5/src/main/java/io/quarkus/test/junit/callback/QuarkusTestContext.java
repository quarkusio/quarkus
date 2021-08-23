package io.quarkus.test.junit.callback;

/**
 * Context object passed to {@link QuarkusTestAfterAllCallback}
 */
public class QuarkusTestContext {

    private final Object testInstance;
    private final Object outerInstance;

    public QuarkusTestContext(Object testInstance, Object outerInstance) {
        this.testInstance = testInstance;
        this.outerInstance = outerInstance;
    }

    public Object getTestInstance() {
        return testInstance;
    }

    public Object getOuterInstance() {
        return outerInstance;
    }
}
