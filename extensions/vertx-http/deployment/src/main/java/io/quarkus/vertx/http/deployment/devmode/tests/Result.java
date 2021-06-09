package io.quarkus.vertx.http.deployment.devmode.tests;

import org.junit.platform.engine.TestExecutionResult;

import io.quarkus.deployment.dev.testing.TestResult;

public class Result {

    private String name;

    private TestExecutionResult.Status status;

    private String exceptionType;

    private String exceptionMessage;

    private long runId;

    public Result() {
    }

    public Result(String name, TestExecutionResult.Status status, String exceptionType, String exceptionMessage, long runId) {
        this.name = name;
        this.status = status;
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
        this.runId = runId;
    }

    public Result(TestResult s) {
        this(s.getDisplayName(),
                s.getTestExecutionResult().getStatus(),
                s.getTestExecutionResult().getThrowable().map(t -> t.getClass().getName()).orElse(null),
                s.getTestExecutionResult().getThrowable().map(Throwable::getMessage).orElse(null),
                s.getRunId());
    }

    public String getName() {
        return name;
    }

    public Result setName(String name) {
        this.name = name;
        return this;
    }

    public TestExecutionResult.Status getStatus() {
        return status;
    }

    public Result setStatus(TestExecutionResult.Status status) {
        this.status = status;
        return this;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public Result setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
        return this;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public Result setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
        return this;
    }

    public long getRunId() {
        return runId;
    }

    public Result setRunId(long runId) {
        this.runId = runId;
        return this;
    }
}
