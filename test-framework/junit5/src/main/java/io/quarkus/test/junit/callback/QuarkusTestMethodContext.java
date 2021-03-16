package io.quarkus.test.junit.callback;

import java.lang.reflect.Method;

/**
 * Context object passed to {@link QuarkusTestBeforeEachCallback} and {@link QuarkusTestAfterEachCallback}
 */
public final class QuarkusTestMethodContext {

    private final Object testInstance;
    private final Method testMethod;

    public QuarkusTestMethodContext(Object testInstance, Method testMethod) {
        this.testInstance = testInstance;
        this.testMethod = testMethod;
    }

    public Object getTestInstance() {
        return testInstance;
    }

    public Method getTestMethod() {
        return testMethod;
    }
}
