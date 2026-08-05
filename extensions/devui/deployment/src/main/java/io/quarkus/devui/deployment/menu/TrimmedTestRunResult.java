package io.quarkus.devui.deployment.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.deployment.dev.testing.TestClassResult;
import io.quarkus.deployment.dev.testing.TestResult;
import io.quarkus.deployment.dev.testing.TestRunResults;

public class TrimmedTestRunResult {

    private final long id;
    private final boolean full;
    private final long started;
    private final long completed;

    private final long passedCount;
    private final long failedCount;
    private final long skippedCount;
    private final long currentPassedCount;
    private final long currentFailedCount;
    private final long currentSkippedCount;
    private final boolean usageDataAvailable;

    private final List<TrimmedTestResult> failing;
    private final List<TrimmedTestResult> passing;
    private final List<TrimmedTestResult> skipped;

    public TrimmedTestRunResult(TestRunResults testRunResults) {
        this(testRunResults, true);
    }

    public TrimmedTestRunResult(TestRunResults testRunResults, boolean usageDataAvailable) {
        this.id = testRunResults.getId();
        this.full = testRunResults.isFull();
        this.started = testRunResults.getStartedTime();
        this.completed = testRunResults.getCompletedTime();
        this.passedCount = testRunResults.getPassedCount();
        this.failedCount = testRunResults.getFailedCount();
        this.skippedCount = testRunResults.getSkippedCount();
        this.currentPassedCount = testRunResults.getCurrentPassedCount();
        this.currentFailedCount = testRunResults.getCurrentFailedCount();
        this.currentSkippedCount = testRunResults.getCurrentSkippedCount();
        this.usageDataAvailable = usageDataAvailable;

        this.failing = extractResults(testRunResults.getCurrentFailing(), TestState.FAILED);
        this.passing = extractResults(testRunResults.getCurrentPassing(), TestState.PASSED);
        this.skipped = extractResults(testRunResults.getResults(), TestState.SKIPPED);
    }

    private enum TestState {
        FAILED,
        PASSED,
        SKIPPED
    }

    private static List<TrimmedTestResult> extractResults(Map<String, TestClassResult> classResults, TestState filterState) {
        List<TrimmedTestResult> results = new ArrayList<>();
        for (TestClassResult classResult : classResults.values()) {
            List<TestResult> tests = switch (filterState) {
                case FAILED -> classResult.getFailing();
                case SKIPPED -> classResult.getSkipped();
                case PASSED -> classResult.getPassing();
            };
            for (TestResult test : tests) {
                if (test.isTest()) {
                    results.add(new TrimmedTestResult(test));
                }
            }
        }
        return results;
    }

    public long getId() {
        return id;
    }

    public boolean isFull() {
        return full;
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

    public boolean isUsageDataAvailable() {
        return usageDataAvailable;
    }

    public List<TrimmedTestResult> getFailing() {
        return failing;
    }

    public List<TrimmedTestResult> getPassing() {
        return passing;
    }

    public List<TrimmedTestResult> getSkipped() {
        return skipped;
    }

    public static class TrimmedTestResult {
        private final String displayName;
        private final String className;
        private final String state;
        private final String message;
        private final long time;

        public TrimmedTestResult(TestResult testResult) {
            this.displayName = testResult.getDisplayName();
            this.className = testResult.getTestClass();
            this.state = testResult.getTestExecutionResult().getStatus().name();
            this.time = testResult.getTime();

            Throwable throwable = testResult.getTestExecutionResult().getThrowable().orElse(null);
            if (throwable != null) {
                this.message = getRootMessage(throwable);
            } else {
                this.message = null;
            }
        }

        private static String getRootMessage(Throwable throwable) {
            Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            Throwable root = throwable;
            while (root.getCause() != null && seen.add(root)) {
                root = root.getCause();
            }
            String msg = root.getMessage();
            if (msg == null) {
                msg = root.getClass().getName();
            }
            return msg;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getClassName() {
            return className;
        }

        public String getState() {
            return state;
        }

        public String getMessage() {
            return message;
        }

        public long getTime() {
            return time;
        }
    }
}
