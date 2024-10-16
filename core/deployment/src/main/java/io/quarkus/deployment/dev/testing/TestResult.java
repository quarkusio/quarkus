package io.quarkus.deployment.dev.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;

import io.quarkus.dev.testing.results.TestResultInterface;

public class TestResult implements TestResultInterface {

    final String displayName;
    final String testClass;
    final List<String> tags;
    final UniqueId uniqueId;
    final TestExecutionResult testExecutionResult;
    final List<String> logOutput;
    final boolean test;
    final long runId;
    final long time;
    final List<Throwable> problems;
    final boolean reportable;

    public TestResult(String displayName, String testClass, List<String> tags, UniqueId uniqueId,
            TestExecutionResult testExecutionResult,
            List<String> logOutput, boolean test, long runId, long time, boolean reportable) {
        this.displayName = displayName;
        this.testClass = testClass;
        this.tags = tags;
        this.uniqueId = uniqueId;
        this.testExecutionResult = testExecutionResult;
        this.logOutput = logOutput;
        this.test = test;
        this.runId = runId;
        this.time = time;
        this.reportable = reportable;
        List<Throwable> problems = new ArrayList<>();
        if (testExecutionResult.getThrowable().isPresent()) {
            Throwable t = testExecutionResult.getThrowable().get();
            while (t != null) {
                problems.add(t);
                t = t.getCause();
            }
        }
        this.problems = Collections.unmodifiableList(problems);
    }

    public TestExecutionResult getTestExecutionResult() {
        return testExecutionResult;
    }

    @Override
    public List<String> getLogOutput() {
        return logOutput;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getTestClass() {
        return testClass;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    public UniqueId getUniqueId() {
        return uniqueId;
    }

    @Override
    public boolean isTest() {
        return test;
    }

    @Override
    public String getId() {
        return uniqueId.toString();
    }

    @Override
    public long getRunId() {
        return runId;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public List<Throwable> getProblems() {
        return problems;
    }

    @Override
    public boolean isReportable() {
        return reportable;
    }

    @Override
    public State getState() {
        return switch (testExecutionResult.getStatus()) {
            case FAILED -> State.FAILED;
            case ABORTED -> State.SKIPPED;
            case SUCCESSFUL -> State.PASSED;
        };
    }
}
