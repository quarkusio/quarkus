package io.quarkus.test.junit.callback;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Context object passed to {@link QuarkusTestBeforeEachCallback} and {@link QuarkusTestAfterEachCallback}
 */
public final class QuarkusTestMethodContext extends QuarkusTestContext {

    private final Method testMethod;

    public QuarkusTestMethodContext(Object testInstance, List<Object> outerInstances, Method testMethod) {
        super(testInstance, outerInstances);
        this.testMethod = testMethod;
    }

    public Method getTestMethod() {
        return testMethod;
    }
}
