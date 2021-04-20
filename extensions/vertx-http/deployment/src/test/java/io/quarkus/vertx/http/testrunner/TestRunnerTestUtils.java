package io.quarkus.vertx.http.testrunner;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;

import io.quarkus.vertx.http.deployment.devmode.tests.TestStatus;
import io.restassured.RestAssured;

/**
 * Utilities for testing the test runner itself
 */
public class TestRunnerTestUtils {

    public static TestStatus waitForFirstRunToComplete() {
        return waitForRun(1);
    }

    public static TestStatus waitForRun(long id) {
        Awaitility.waitAtMost(1, TimeUnit.MINUTES).pollInterval(50, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                TestStatus ts = RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
                return ts.getLastRun() == id;
            }
        });
        return RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
    }

    public static String appProperties(String... props) {
        return "quarkus.test.continuous-testing=enabled\nquarkus.test.console=false\nquarkus.test.display-test-output=true\n"
                + String.join("\n", Arrays.asList(props));
    }
}
