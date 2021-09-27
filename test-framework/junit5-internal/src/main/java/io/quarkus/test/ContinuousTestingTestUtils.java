package io.quarkus.test;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import io.quarkus.dev.testing.ContinuousTestingSharedStateManager;

/**
 * Utilities for testing the test runner itself
 */
public class ContinuousTestingTestUtils {

    long runToWaitFor = 1;

    public TestStatus waitForNextCompletion() {
        try {
            Awaitility.waitAtMost(1, TimeUnit.MINUTES).pollInterval(50, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    ContinuousTestingSharedStateManager.State ts = ContinuousTestingSharedStateManager.getLastState();
                    if (ts.lastRun > runToWaitFor) {
                        //sometimes we can run spuriously, because of the way file systems work
                        //we just roll with it, and take the updated results
                        runToWaitFor = ts.lastRun;
                    }
                    boolean runComplete = ts.lastRun == runToWaitFor;
                    if (runComplete && ts.inProgress) {
                        //there is a small chance of a race, where changes are picked up twice, due to how filesystems work
                        //this works around it by waiting for the next run
                        runToWaitFor = ts.lastRun + 1;
                        return false;
                    } else if (runComplete) {
                        runToWaitFor++;
                    }
                    return runComplete;
                }
            });
        } catch (Exception e) {
            ContinuousTestingSharedStateManager.State ts = ContinuousTestingSharedStateManager.getLastState();
            throw new ConditionTimeoutException("Failed to wait for test run " + runToWaitFor + " " + ts, e);
        }
        var s = ContinuousTestingSharedStateManager.getLastState();
        return new TestStatus(s.lastRun, s.running ? s.lastRun + 1 : -1, s.run, s.currentPassed, s.currentFailed,
                s.currentSkipped, s.passed, s.failed, s.skipped);
    }

    public static String appProperties(String... props) {
        return "quarkus.test.continuous-testing=enabled\nquarkus.test.display-test-output=true\nquarkus.test.basic-console=true\nquarkus.test.disable-console-input=true\n"
                + String.join("\n", Arrays.asList(props));
    }

    public static class TestStatus {

        private long lastRun;

        private long running;

        private long testsRun = -1;

        private long testsPassed = -1;

        private long testsFailed = -1;

        private long testsSkipped = -1;

        private long totalTestsPassed = -1;

        private long totalTestsFailed = -1;

        private long totalTestsSkipped = -1;

        public TestStatus() {
        }

        public TestStatus(long lastRun, long running, long testsRun, long testsPassed, long testsFailed, long testsSkipped,
                long totalTestsPassed, long totalTestsFailed, long totalTestsSkipped) {
            this.lastRun = lastRun;
            this.running = running;
            this.testsRun = testsRun;
            this.testsPassed = testsPassed;
            this.testsFailed = testsFailed;
            this.testsSkipped = testsSkipped;
            this.totalTestsPassed = totalTestsPassed;
            this.totalTestsFailed = totalTestsFailed;
            this.totalTestsSkipped = totalTestsSkipped;

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

        public long getTotalTestsPassed() {
            return totalTestsPassed;
        }

        public TestStatus setTotalTestsPassed(long totalTestsPassed) {
            this.totalTestsPassed = totalTestsPassed;
            return this;
        }

        public long getTotalTestsFailed() {
            return totalTestsFailed;
        }

        public TestStatus setTotalTestsFailed(long totalTestsFailed) {
            this.totalTestsFailed = totalTestsFailed;
            return this;
        }

        public long getTotalTestsSkipped() {
            return totalTestsSkipped;
        }

        public TestStatus setTotalTestsSkipped(long totalTestsSkipped) {
            this.totalTestsSkipped = totalTestsSkipped;
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
}
