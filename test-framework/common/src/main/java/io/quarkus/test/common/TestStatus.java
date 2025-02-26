package io.quarkus.test.common;

public class TestStatus {

    private final Throwable testErrorCause;

    public TestStatus(Throwable testErrorCause) {
        this.testErrorCause = testErrorCause;
    }

    /**
     * @return the error cause that was thrown during either `BeforeAll`, `BeforeEach`, test method, `AfterAll` or
     *         `AfterEach` phases.
     */
    public Throwable getTestErrorCause() {
        return testErrorCause;
    }

    /**
     * @return whether the test has failed.
     */
    public boolean isTestFailed() {
        return getTestErrorCause() != null;
    }
}
