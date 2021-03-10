package io.quarkus.deployment.dev.testing;

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

    public TestResult(String displayName, String testClass, UniqueId uniqueId, TestExecutionResult testExecutionResult,
            List<String> logOutput, boolean test, long runId) {
        this.displayName = displayName;
        this.testClass = testClass;
        this.uniqueId = uniqueId;
        this.testExecutionResult = testExecutionResult;
        this.logOutput = logOutput;
        this.test = test;
        this.runId = runId;
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
}
