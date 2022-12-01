package io.quarkus.vertx.http.deployment.devmode.tests;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.dev.testing.TestClassResult;
import io.quarkus.deployment.dev.testing.TestResult;

public class ClassResult implements Comparable<ClassResult> {
    String className;
    List<Result> passing;
    List<Result> failing;
    List<Result> skipped;
    long latestRunId;

    public ClassResult(String className, List<Result> passing, List<Result> failing, List<Result> skipped) {
        this.className = className;
        this.passing = passing;
        this.failing = failing;
        this.skipped = skipped;
        long runId = 0;
        for (Result i : passing) {
            runId = Math.max(i.getRunId(), runId);
        }
        for (Result i : failing) {
            runId = Math.max(i.getRunId(), runId);
        }
        latestRunId = runId;
    }

    public ClassResult(TestClassResult res) {
        this.className = res.getClassName();
        this.failing = res.getFailing().stream().map(Result::new).collect(Collectors.toList());
        this.passing = res.getPassing().stream().filter(TestResult::isTest).map(Result::new).collect(Collectors.toList());
        this.skipped = res.getSkipped().stream().filter(TestResult::isTest).map(Result::new).collect(Collectors.toList());
        this.latestRunId = res.getLatestRunId();
    }

    public ClassResult() {

    }

    public String getClassName() {
        return className;
    }

    public List<Result> getPassing() {
        return passing;
    }

    public List<Result> getFailing() {
        return failing;
    }

    public List<Result> getSkipped() {
        return skipped;
    }

    public long getLatestRunId() {
        return latestRunId;
    }

    public ClassResult setClassName(String className) {
        this.className = className;
        return this;
    }

    public ClassResult setPassing(List<Result> passing) {
        this.passing = passing;
        return this;
    }

    public ClassResult setFailing(List<Result> failing) {
        this.failing = failing;
        return this;
    }

    public ClassResult setSkipped(List<Result> skipped) {
        this.skipped = skipped;
        return this;
    }

    public ClassResult setLatestRunId(long latestRunId) {
        this.latestRunId = latestRunId;
        return this;
    }

    @Override
    public int compareTo(ClassResult o) {
        return className.compareTo(o.className);
    }

}
