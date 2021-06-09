package io.quarkus.deployment.dev.testing;

import java.util.ArrayList;
import java.util.List;

public class TestClassResult implements Comparable<TestClassResult> {
    final String className;
    final List<TestResult> passing;
    final List<TestResult> failing;
    final List<TestResult> skipped;
    final long latestRunId;

    public TestClassResult(String className, List<TestResult> passing, List<TestResult> failing, List<TestResult> skipped) {
        this.className = className;
        this.passing = passing;
        this.failing = failing;
        this.skipped = skipped;
        long runId = 0;
        for (TestResult i : passing) {
            runId = Math.max(i.runId, runId);
        }
        for (TestResult i : failing) {
            runId = Math.max(i.runId, runId);
        }
        latestRunId = runId;
    }

    public String getClassName() {
        return className;
    }

    public List<TestResult> getPassing() {
        return passing;
    }

    public List<TestResult> getFailing() {
        return failing;
    }

    public List<TestResult> getSkipped() {
        return skipped;
    }

    public long getLatestRunId() {
        return latestRunId;
    }

    @Override
    public int compareTo(TestClassResult o) {
        return className.compareTo(o.className);
    }

    public List<TestResult> getResults() {
        List<TestResult> ret = new ArrayList<>();
        ret.addAll(passing);
        ret.addAll(failing);
        ret.addAll(skipped);
        return ret;
    }
}
