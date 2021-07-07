package io.quarkus.vertx.http.testrunner;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import io.quarkus.vertx.http.deployment.devmode.tests.TestStatus;
import io.restassured.RestAssured;

/**
 * Utilities for testing the test runner itself
 */
public class ContinuousTestingTestUtils {

    long runtToWaitFor = 1;

    public TestStatus waitForNextCompletion() {
        try {
            Awaitility.waitAtMost(1, TimeUnit.MINUTES).pollInterval(50, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    TestStatus ts = RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
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
        } catch (Exception e) {
            TestStatus ts = RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
            throw new ConditionTimeoutException("Failed to wait for test run" + runtToWaitFor + " " + ts);
        }
        return RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
    }

    public static String appProperties(String... props) {
        return "quarkus.test.continuous-testing=enabled\nquarkus.test.display-test-output=true\nquarkus.test.basic-console=true\nquarkus.test.disable-console-input=true\n"
                + String.join("\n", Arrays.asList(props));
    }
}
