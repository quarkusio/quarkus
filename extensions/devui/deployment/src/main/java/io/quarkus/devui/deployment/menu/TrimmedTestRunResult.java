package io.quarkus.devui.deployment.menu;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.deployment.dev.testing.TestClassResult;
import io.quarkus.deployment.dev.testing.TestRunResults;

public class TrimmedTestRunResult {

    /**
     * The run id
     */
    private final long id;

    /**
     * If this ran all tests, or just the modified ones
     */
    private final boolean full;

    private final long started;
    private final long completed;

    private final Map<String, TestClassResult> currentFailing = new HashMap<>();
    private final Map<String, TestClassResult> historicFailing = new HashMap<>();
    private final Map<String, TestClassResult> currentPassing = new HashMap<>();
    private final Map<String, TestClassResult> historicPassing = new HashMap<>();

    private final long passedCount;
    private final long failedCount;
    private final long skippedCount;
    private final long currentPassedCount;
    private final long currentFailedCount;
    private final long currentSkippedCount;

    public TrimmedTestRunResult(TestRunResults testRunResults) {
        this.id = testRunResults.getId();
        this.full = testRunResults.isFull();
        this.started = testRunResults.getStartedTime();
        this.completed = testRunResults.getCompletedTime();
        this.currentFailing.putAll(testRunResults.getCurrentFailing());
        this.historicFailing.putAll(testRunResults.getHistoricFailing());
        this.currentPassing.putAll(testRunResults.getCurrentPassing());
        this.historicPassing.putAll(testRunResults.getHistoricPassing());
        this.passedCount = testRunResults.getPassedCount();
        this.failedCount = testRunResults.getFailedCount();
        this.skippedCount = testRunResults.getSkippedCount();
        this.currentPassedCount = testRunResults.getCurrentPassedCount();
        this.currentFailedCount = testRunResults.getCurrentFailedCount();
        this.currentSkippedCount = testRunResults.getCurrentSkippedCount();
    }

    public long getId() {
        return id;
    }

    public boolean isFull() {
        return full;
    }

    public Map<String, TestClassResult> getCurrentFailing() {
        return currentFailing;
    }

    public Map<String, TestClassResult> getHistoricFailing() {
        return historicFailing;
    }

    public Map<String, TestClassResult> getCurrentPassing() {
        return currentPassing;
    }

    public Map<String, TestClassResult> getHistoricPassing() {
        return historicPassing;
    }

    public long getStartedTime() {
        return started;
    }

    public long getCompletedTime() {
        return completed;
    }

    public long getTotalTime() {
        return completed - started;
    }

    public long getPassedCount() {
        return passedCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public long getSkippedCount() {
        return skippedCount;
    }

    public long getCurrentPassedCount() {
        return currentPassedCount;
    }

    public long getCurrentFailedCount() {
        return currentFailedCount;
    }

    public long getCurrentSkippedCount() {
        return currentSkippedCount;
    }

}
