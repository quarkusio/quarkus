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

    /**
     * Toggles test output
     *
     * @return <code>true</code> if this change to test output mode
     */
    boolean toggleTestOutput();

    /**
     * Toggles instrumentation based reload.
     * 
     * @return <code>true</code> if this change to do instrumentation based reload
     */
    boolean toggleInstrumentation();

    /**
     * Toggles instrumentation based reload.
     *
     * @return <code>true</code> if this change to do instrumentation based reload
     */
    boolean toggleLiveReloadEnabled();

    /**
     * Print the current results and failures
     */
    void printFullResults();

    /**
     *
     * @return <code>true</code> if broken only mode is enabled
     */
    boolean isBrokenOnlyMode();

    /**
     *
     * @return <code>true</code> if test output is enabled
     */
    boolean isDisplayTestOutput();

    /**
     *
     * @return <code>true</code> if live reload is enabled
     */
    boolean isInstrumentationEnabled();

    /**
     *
     * @return <code>true</code> if live reload is enabled
     */
    boolean isLiveReloadEnabled();
}
