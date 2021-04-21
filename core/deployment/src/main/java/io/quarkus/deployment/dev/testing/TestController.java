package io.quarkus.deployment.dev.testing;

public interface TestController {

    /**
     * The current test state
     */
    TestState currentState();

    /**
     * Runs all tests
     */
    void runAllTests();

    /**
     * Sets if test output should be displayed in the logs
     * 
     * @param displayTestOutput
     */
    void setDisplayTestOutput(boolean displayTestOutput);

    /**
     * re-runs all tests that are currently in a failed state
     */
    void runFailedTests();

    /**
     * Toggles 'broken only' mode, where only failing tests are run
     *
     * @return <code>true</code> if this change enabled broken only mode
     */
    boolean toggleBrokenOnlyMode();
}
