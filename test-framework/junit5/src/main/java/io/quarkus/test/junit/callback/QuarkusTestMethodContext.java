package io.quarkus.test.junit.callback;

import java.lang.reflect.Method;

/**
 * Context object passed to {@link QuarkusTestBeforeEachCallback} and {@link QuarkusTestAfterEachCallback}
 */
public final class QuarkusTestMethodContext extends QuarkusTestContext {

    private final Method testMethod;

    public QuarkusTestMethodContext(Object testInstance, Object outerInstance, Method testMethod) {
        super(testInstance, outerInstance);
        this.testMethod = testMethod;
    }

    public Method getTestMethod() {
        return testMethod;
    }
}
