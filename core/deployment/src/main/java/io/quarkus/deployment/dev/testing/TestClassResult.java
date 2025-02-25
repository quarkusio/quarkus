package io.quarkus.deployment.dev.testing;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.dev.testing.results.TestClassResultInterface;
import io.quarkus.dev.testing.results.TestResultInterface;

public class TestClassResult implements TestClassResultInterface {
    final String className;
    final List<TestResult> passing;
    final List<TestResult> failing;
    final List<TestResult> skipped;
    final long latestRunId;
    final long time;

    public TestClassResult(
            String className,
            List<TestResult> passing,
            List<TestResult> failing,
            List<TestResult> skipped,
            long time) {
        this.className = className;
        this.passing = passing;
        this.failing = failing;
        this.skipped = skipped;
        this.time = time;
        long runId = 0;
        for (TestResultInterface i : passing) {
            runId = Math.max(i.getRunId(), runId);
        }
        for (TestResultInterface i : failing) {
            runId = Math.max(i.getRunId(), runId);
        }
        latestRunId = runId;
    }

    @Override
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

    public long getTime() {
        return time;
    }

    @Override
    public List<TestResult> getResults() {
        List<TestResult> ret = new ArrayList<>();
        ret.addAll(passing);
        ret.addAll(failing);
        ret.addAll(skipped);
        return ret;
    }

}
