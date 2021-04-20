package io.quarkus.vertx.http.deployment.devmode.tests;

public class TestStatus {

    private long lastRun;

    private long running;

    private long testsRun = -1;

    private long testsPassed = -1;

    private long testsFailed = -1;

    private long testsSkipped = -1;

    public TestStatus() {
    }

    public TestStatus(long lastRun, long running, long testsRun, long testsPassed, long testsFailed, long testsSkipped) {
        this.lastRun = lastRun;
        this.running = running;
        this.testsRun = testsRun;
        this.testsPassed = testsPassed;
        this.testsFailed = testsFailed;
        this.testsSkipped = testsSkipped;
    }

    public long getLastRun() {
        return lastRun;
    }

    public TestStatus setLastRun(long lastRun) {
        this.lastRun = lastRun;
        return this;
    }

    public long getRunning() {
        return running;
    }

    public TestStatus setRunning(long running) {
        this.running = running;
        return this;
    }

    public long getTestsRun() {
        return testsRun;
    }

    public TestStatus setTestsRun(long testsRun) {
        this.testsRun = testsRun;
        return this;
    }

    public long getTestsPassed() {
        return testsPassed;
    }

    public TestStatus setTestsPassed(long testsPassed) {
        this.testsPassed = testsPassed;
        return this;
    }

    public long getTestsFailed() {
        return testsFailed;
    }

    public TestStatus setTestsFailed(long testsFailed) {
        this.testsFailed = testsFailed;
        return this;
    }

    public long getTestsSkipped() {
        return testsSkipped;
    }

    public TestStatus setTestsSkipped(long testsSkipped) {
        this.testsSkipped = testsSkipped;
        return this;
    }

    @Override
    public String toString() {
        return "TestStatus{" +
                "lastRun=" + lastRun +
                ", running=" + running +
                ", testsRun=" + testsRun +
                ", testsPassed=" + testsPassed +
                ", testsFailed=" + testsFailed +
                ", testsSkipped=" + testsSkipped +
                '}';
    }
}
