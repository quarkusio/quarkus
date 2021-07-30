package io.quarkus.deployment.dev.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;

public class TestResult {

    final String displayName;
    final String testClass;
    final UniqueId uniqueId;
    final TestExecutionResult testExecutionResult;
    final List<String> logOutput;
    final boolean test;
    final long runId;
    final long time;
    final List<Throwable> problems;

    public TestResult(String displayName, String testClass, UniqueId uniqueId, TestExecutionResult testExecutionResult,
            List<String> logOutput, boolean test, long runId, long time) {
        this.displayName = displayName;
        this.testClass = testClass;
        this.uniqueId = uniqueId;
        this.testExecutionResult = testExecutionResult;
        this.logOutput = logOutput;
        this.test = test;
        this.runId = runId;
        this.time = time;
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

    public List<String> getLogOutput() {
        return logOutput;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTestClass() {
        return testClass;
    }

    public UniqueId getUniqueId() {
        return uniqueId;
    }

    public boolean isTest() {
        return test;
    }

    public long getRunId() {
        return runId;
    }

    public long getTime() {
        return time;
    }

    public List<Throwable> getProblems() {
        return problems;
    }
}
