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

    public static TestStatus waitForFirstRunToComplete() {
        return waitForRun(1);
    }

    public static TestStatus waitForRun(long id) {
        try {
            Awaitility.waitAtMost(1, TimeUnit.MINUTES).pollInterval(50, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    TestStatus ts = RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
                    if (ts.getLastRun() > id) {
                        throw new RuntimeException(
                                "Waiting for run " + id + " but run " + ts.getLastRun() + " has already occurred");
                    }
                    return ts.getLastRun() == id;
                }
            });
        } catch (Exception e) {
            TestStatus ts = RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
            throw new ConditionTimeoutException("Failed to wait for test run" + id + " " + ts);
        }
        return RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
    }

    public static String appProperties(String... props) {
        return "quarkus.test.continuous-testing=enabled\nquarkus.test.display-test-output=true\nquarkus.test.basic-console=true\nquarkus.test.disable-console-input=true\n"
                + String.join("\n", Arrays.asList(props));
    }
}
