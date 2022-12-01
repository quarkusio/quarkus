package io.quarkus.maven.it.continuoustesting;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilities for testing the test runner itself
 */
public class ContinuousTestingMavenTestUtils {

    static final URL DEFAULT;

    static {
        try {
            DEFAULT = new URL("http://localhost:8080/q/dev/io.quarkus.quarkus-vertx-http/tests/status");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    long runtToWaitFor = 1;
    ObjectMapper objectMapper = new ObjectMapper();
    final URL url;

    public ContinuousTestingMavenTestUtils() {
        this(DEFAULT);
    }

    public ContinuousTestingMavenTestUtils(URL url) {
        this.url = url;
    }

    public TestStatus waitForNextCompletion() {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            Awaitility.waitAtMost(1, TimeUnit.MINUTES).pollInterval(50, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    TestStatus ts = objectMapper.readValue(url, TestStatus.class);
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
                }
            });
            return objectMapper.readValue(url, TestStatus.class);
        } catch (Exception e) {
            TestStatus ts = null;
            try {
                ts = objectMapper.readValue(url, TestStatus.class);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            throw new ConditionTimeoutException("Failed to wait for test run" + runtToWaitFor + " " + ts);
        }
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
