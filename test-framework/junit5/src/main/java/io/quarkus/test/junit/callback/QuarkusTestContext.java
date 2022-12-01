package io.quarkus.test.junit.callback;

import java.util.List;

import io.quarkus.test.common.TestStatus;

/**
 * Context object passed to {@link QuarkusTestAfterAllCallback}
 */
public class QuarkusTestContext {

    private final Object testInstance;
    private final List<Object> outerInstances;
    private final TestStatus testStatus;

    public QuarkusTestContext(Object testInstance, List<Object> outerInstances, Throwable testErrorCause) {
        this.testInstance = testInstance;
        this.outerInstances = outerInstances;
        this.testStatus = new TestStatus(testErrorCause);
    }

    public Object getTestInstance() {
        return testInstance;
    }

    public List<Object> getOuterInstances() {
        return outerInstances;
    }

    /**
     * @return the failure result that is thrown during either `BeforeAll`, `BeforeEach`, test method, `AfterAll` or
     *         `AfterEach` phases.
     */
    public TestStatus getTestStatus() {
        return testStatus;
    }
}
