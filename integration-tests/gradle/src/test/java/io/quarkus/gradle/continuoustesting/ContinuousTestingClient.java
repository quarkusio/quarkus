package io.quarkus.gradle.continuoustesting;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.devui.tests.DevUIJsonRPCTest;

/**
 * Utilities for testing the test runner itself
 */
// copy of `ContinuousTestingMavenTestUtils`, perhaps we should unify them somewhere?
public class ContinuousTestingClient {
    private static final int DEFAULT_PORT = 8080;

    long runtToWaitFor = 1;
    final String host;

    protected static String getDefaultHost(int port) {
        return "http://localhost:" + port;
    }

    public ContinuousTestingClient() {
        this(getDefaultHost(DEFAULT_PORT));
    }

    public ContinuousTestingClient(int port) {
        this(getDefaultHost(port));
    }

    public ContinuousTestingClient(String host) {
        this.host = host;
    }

    public TestStatus waitForNextCompletion() {
        try {
            Awaitility.waitAtMost(2, TimeUnit.MINUTES).pollInterval(200, TimeUnit.MILLISECONDS).until(() -> {
                TestStatus ts = getTestStatus();
                if (ts.getLastRun() > runtToWaitFor) {
                    throw new RuntimeException(
                            "Waiting for run " + runtToWaitFor + " but run " + ts.getLastRun() + " has already occurred");
                }
                boolean runComplete = ts.getLastRun() == runtToWaitFor;
                if (runComplete && ts.getRunning() > 0) {
                    //there is a small chance of a race, where changes are picked up twice, due to how filesystems work
                    //this works around it by waiting for the next run
                    runtToWaitFor = ts.getRunning();
                    return false;
                } else if (runComplete) {
                    runtToWaitFor++;
                }
                return runComplete;
            });
            return getTestStatus();
        } catch (Exception e) {
            TestStatus ts;
            try {
                ts = getTestStatus();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            throw new ConditionTimeoutException("Failed to wait for test run " + runtToWaitFor + " " + ts, e);
        }
    }

    private TestStatus getTestStatus() {
        DevUIJsonRPCTest devUIJsonRPCTest = new DevUIJsonRPCTest("devui-continuous-testing", this.host);
        try {

            TypeReference<Map<String, Long>> typeRef = new TypeReference<Map<String, Long>>() {
            };
            Map<String, Long> testStatus = devUIJsonRPCTest.executeJsonRPCMethod(typeRef, "getStatus");

            long lastRun = testStatus.getOrDefault("lastRun", -1L);
            long running = testStatus.getOrDefault("running", -1L);
            long testsRun = testStatus.getOrDefault("testsRun", -1L);
            long testsPassed = testStatus.getOrDefault("testsPassed", -1L);
            long testsFailed = testStatus.getOrDefault("testsFailed", -1L);
            long testsSkipped = testStatus.getOrDefault("testsSkipped", -1L);
            long totalTestsPassed = testStatus.getOrDefault("totalTestsPassed", -1L);
            long totalTestsFailed = testStatus.getOrDefault("totalTestsFailed", -1L);
            long totalTestsSkipped = testStatus.getOrDefault("totalTestsSkipped", -1L);

            return new TestStatus(lastRun, running, testsRun, testsPassed, testsFailed, testsSkipped, totalTestsPassed,
                    totalTestsFailed, totalTestsSkipped);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            super();
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

        public void setLastRun(long lastRun) {
            this.lastRun = lastRun;
        }

        public long getRunning() {
            return running;
        }

        public void setRunning(long running) {
            this.running = running;
        }

        public long getTestsRun() {
            return testsRun;
        }

        public void setTestsRun(long testsRun) {
            this.testsRun = testsRun;
        }

        public long getTestsPassed() {
            return testsPassed;
        }

        public void setTestsPassed(long testsPassed) {
            this.testsPassed = testsPassed;
        }

        public long getTestsFailed() {
            return testsFailed;
        }

        public void setTestsFailed(long testsFailed) {
            this.testsFailed = testsFailed;
        }

        public long getTestsSkipped() {
            return testsSkipped;
        }

        public void setTestsSkipped(long testsSkipped) {
            this.testsSkipped = testsSkipped;
        }

        public long getTotalTestsPassed() {
            return totalTestsPassed;
        }

        public void setTotalTestsPassed(long totalTestsPassed) {
            this.totalTestsPassed = totalTestsPassed;
        }

        public long getTotalTestsFailed() {
            return totalTestsFailed;
        }

        public void setTotalTestsFailed(long totalTestsFailed) {
            this.totalTestsFailed = totalTestsFailed;
        }

        public long getTotalTestsSkipped() {
            return totalTestsSkipped;
        }

        public void setTotalTestsSkipped(long totalTestsSkipped) {
            this.totalTestsSkipped = totalTestsSkipped;
        }

        @Override
        public String toString() {
            return "TestStatus{" + "lastRun=" + lastRun + ", running=" + running + ", testsRun=" + testsRun + ", testsPassed="
                    + testsPassed + ", testsFailed=" + testsFailed + ", testsSkipped=" + testsSkipped + ", totalTestsPassed="
                    + totalTestsPassed + ", totalTestsFailed=" + totalTestsFailed + ", totalTestsSkipped=" + totalTestsSkipped
                    + '}';
        }
    }
}
