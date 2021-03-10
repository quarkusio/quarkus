package io.quarkus.vertx.http.deployment.devmode.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.testing.TestClassResult;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TestsProcessor {
    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem results(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return null;
        }
        return new DevConsoleTemplateInfoBuildItem("tests", ts.get());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleRouteBuildItem handleTestStatus(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return null;
        }
        //GET tests/status
        //DISABLED, RUNNING (run id), RUN (run id, start time, nextRunQueued)
        //GET tests/results

        return new DevConsoleRouteBuildItem("tests/status", "GET", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                jsonResponse(event);
                TestSupport.RunStatus status = ts.get().getStatus();
                TestStatus testStatus = new TestStatus();
                testStatus.setLastRun(status.getLastRun());
                testStatus.setRunning(status.getRunning());
                if (status.getLastRun() > 0) {
                    TestRunResults result = ts.get().getResults();
                    testStatus.setTestsFailed(result.getTestsFailed());
                    testStatus.setTestsPassed(result.getTestsPassed());
                    testStatus.setTestsSkipped(result.getTestsSkipped());
                    testStatus.setTestsRun(result.getTestsFailed() + result.getTestsPassed());
                }
                event.response().end(JsonObject.mapFrom(testStatus).encode());
            }
        });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleRouteBuildItem toggleTestRunner(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return null;
        }
        //GET tests/status
        //DISABLED, RUNNING (run id), RUN (run id, start time, nextRunQueued)
        //GET tests/results

        return new DevConsoleRouteBuildItem("tests/toggle", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (ts.get().isStarted()) {
                    ts.get().stop();
                } else {
                    ts.get().start();
                }
            }
        });
    }

    private boolean testsDisabled(LaunchModeBuildItem launchModeBuildItem, Optional<TestSupport> ts) {
        return !ts.isPresent() || launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleRouteBuildItem runAllTests(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return null;
        }
        //GET tests/status
        //DISABLED, RUNNING (run id), RUN (run id, start time, nextRunQueued)
        //GET tests/results

        return new DevConsoleRouteBuildItem("tests/runall", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                ts.get().runAllTests();
            }
        });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleRouteBuildItem handleTestResult(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return null;
        }
        //GET tests/status
        //DISABLED, RUNNING (run id), RUN (run id, start time, nextRunQueued)
        //GET tests/results

        return new DevConsoleRouteBuildItem("tests/result", "GET", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                TestRunResults testRunResults = ts.get().getResults();
                if (testRunResults == null) {
                    event.response().setStatusCode(204).end();
                } else {

                    jsonResponse(event);
                    Map<String, ClassResult> results = new HashMap<>();
                    for (Map.Entry<String, TestClassResult> entry : testRunResults.getResults().entrySet()) {
                        results.put(entry.getKey(), new ClassResult(entry.getValue()));
                    }
                    SuiteResult result = new SuiteResult(results);
                    event.response().end(JsonObject.mapFrom(result).encode());
                }
            }
        });
    }

    public MultiMap jsonResponse(RoutingContext event) {
        return event.response().headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
    }
}
