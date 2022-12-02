package io.quarkus.deployment.dev.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.deployment.dev.ClassScanResult;

public class TestRunResults {

    /**
     * The run id
     */
    private final long id;

    /**
     * The change that triggered this run, may be null.
     */
    private final ClassScanResult trigger;

    /**
     * If this ran all tests, or just the modified ones
     */
    private final boolean full;

    private final long started;
    private final long completed;

    private final Map<String, TestClassResult> results;
    private final Map<String, TestClassResult> currentFailing = new HashMap<>();
    private final Map<String, TestClassResult> historicFailing = new HashMap<>();
    private final Map<String, TestClassResult> currentPassing = new HashMap<>();
    private final Map<String, TestClassResult> historicPassing = new HashMap<>();
    private final List<TestClassResult> failing = new ArrayList<>();
    private final List<TestClassResult> passing = new ArrayList<>();
    private final List<TestClassResult> skipped = new ArrayList<>();
    private final long passedCount;
    private final long failedCount;
    private final long skippedCount;
    private final long currentPassedCount;
    private final long currentFailedCount;
    private final long currentSkippedCount;

    public TestRunResults(long id, ClassScanResult trigger, boolean full, long started, long completed,
            Map<String, TestClassResult> results) {
        this.id = id;
        this.trigger = trigger;
        this.full = full;
        this.started = started;
        this.completed = completed;
        this.results = new HashMap<>(results);
        long passedCount = 0;
        long failedCount = 0;
        long skippedCount = 0;
        long currentPassedCount = 0;
        long currentFailedCount = 0;
        long currentSkippedCount = 0;
        for (Map.Entry<String, TestClassResult> i : results.entrySet()) {
            passedCount += i.getValue().getPassing().stream().filter(TestResult::isTest).count();
            failedCount += i.getValue().getFailing().stream().filter(TestResult::isTest).count();
            skippedCount += i.getValue().getSkipped().stream().filter(TestResult::isTest).count();
            currentPassedCount += i.getValue().getPassing().stream().filter(s -> s.isTest() && s.getRunId() == id).count();
            currentFailedCount += i.getValue().getFailing().stream().filter(s -> s.isTest() && s.getRunId() == id).count();
            currentSkippedCount += i.getValue().getSkipped().stream().filter(s -> s.isTest() && s.getRunId() == id).count();
            boolean current = i.getValue().getLatestRunId() == id;
            if (current) {
                if (!i.getValue().getFailing().isEmpty()) {
                    currentFailing.put(i.getKey(), i.getValue());
                    failing.add(i.getValue());
                } else if (!i.getValue().getPassing().isEmpty()) {
                    currentPassing.put(i.getKey(), i.getValue());
                    passing.add(i.getValue());
                } else {
                    skipped.add(i.getValue());
                }
            } else {
                if (!i.getValue().getFailing().isEmpty()) {
                    historicFailing.put(i.getKey(), i.getValue());
                    failing.add(i.getValue());
                } else if (!i.getValue().getPassing().isEmpty()) {
                    historicPassing.put(i.getKey(), i.getValue());
                    passing.add(i.getValue());
                } else {
                    skipped.add(i.getValue());
                }
            }
        }
        Collections.sort(passing);
        Collections.sort(failing);
        Collections.sort(skipped);
        this.failedCount = failedCount;
        this.passedCount = passedCount;
        this.skippedCount = skippedCount;
        this.currentFailedCount = currentFailedCount;
        this.currentPassedCount = currentPassedCount;
        this.currentSkippedCount = currentSkippedCount;
    }

    public long getId() {
        return id;
    }

    public ClassScanResult getTrigger() {
        return trigger;
    }

    public boolean isFull() {
        return full;
    }

    public Map<String, TestClassResult> getResults() {
        return Collections.unmodifiableMap(results);
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

    public List<TestClassResult> getFailing() {
        return failing;
    }

    public List<TestClassResult> getPassing() {
        return passing;
    }

    public List<TestClassResult> getSkipped() {
        return skipped;
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

    public long getTotalCount() {
        return getPassedCount() + getFailedCount() + getSkippedCount();
    }

    public long getCurrentTotalCount() {
        return getCurrentPassedCount() + getCurrentFailedCount() + getCurrentSkippedCount();
    }
}
