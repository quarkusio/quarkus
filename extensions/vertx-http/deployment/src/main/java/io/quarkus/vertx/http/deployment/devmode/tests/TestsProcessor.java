package io.quarkus.vertx.http.deployment.devmode.tests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.testing.TestClassResult;
import io.quarkus.deployment.dev.testing.TestListenerBuildItem;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.console.ContinuousTestingWebSocketListener;
import io.quarkus.vertx.http.runtime.devmode.DevConsoleRecorder;
import io.quarkus.vertx.http.runtime.devmode.Json;
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

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    public void setupTestRoutes(
            DevConsoleRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<RouteBuildItem> routeBuildItemBuildProducer,
            BuildProducer<TestListenerBuildItem> testListenerBuildItemBuildProducer) throws IOException {
        DevModeType devModeType = launchModeBuildItem.getDevModeType().orElse(null);
        if (devModeType == null || !devModeType.isContinuousTestingSupported()) {
            return;
        }

        if (TestSupport.instance().isPresent()) {
            // Add continuous testing
            routeBuildItemBuildProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .route("dev/test")
                    .handler(recorder.continousTestHandler())
                    .build());
            testListenerBuildItemBuildProducer.produce(new TestListenerBuildItem(new ContinuousTestingWebSocketListener()));
        }

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
                    testStatus.setTestsFailed(result.getCurrentFailedCount());
                    testStatus.setTestsPassed(result.getCurrentPassedCount());
                    testStatus.setTestsSkipped(result.getCurrentSkippedCount());
                    testStatus.setTestsRun(result.getFailedCount() + result.getPassedCount());
                    testStatus.setTotalTestsFailed(result.getFailedCount());
                    testStatus.setTotalTestsPassed(result.getPassedCount());
                    testStatus.setTotalTestsSkipped(result.getSkippedCount());
                }
                event.response().end(JsonObject.mapFrom(testStatus).encode());
            }
        });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void toggleTestRunner(LaunchModeBuildItem launchModeBuildItem, BuildProducer<DevConsoleRouteBuildItem> routeProducer) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return;
        }
        routeProducer.produce(new DevConsoleRouteBuildItem("tests/toggle", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (ts.get().isStarted()) {
                    ts.get().stop();
                } else {
                    ts.get().start();
                }

                Json.JsonObjectBuilder object = Json.object();
                object.put("running", ts.get().isRunning());
                event.response().putHeader("Content-Type", "application/json; charset=utf-8").end(object.build());
            }
        }));
        routeProducer.produce(new DevConsoleRouteBuildItem("tests/toggle-broken-only", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                boolean brokenOnlyMode = ts.get().toggleBrokenOnlyMode();
                Json.JsonObjectBuilder object = Json.object();
                object.put("brokenOnlyMode", brokenOnlyMode);
                event.response().putHeader("Content-Type", "application/json; charset=utf-8").end(object.build());
            }
        }));
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
        return new DevConsoleRouteBuildItem("tests/runall", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                ts.get().runAllTests();
            }
        });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleRouteBuildItem runFailedTests(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return null;
        }
        return new DevConsoleRouteBuildItem("tests/toggle-test-output", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {

                boolean isTestOutput = ts.get().toggleTestOutput();
                Json.JsonObjectBuilder object = Json.object();
                object.put("isTestOutput", isTestOutput);
                event.response().putHeader("Content-Type", "application/json; charset=utf-8").end(object.build());
            }
        });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleRouteBuildItem toggleTestOutput(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return null;
        }
        return new DevConsoleRouteBuildItem("tests/runfailed", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                ts.get().runFailedTests();
            }
        });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleRouteBuildItem printfailures(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return null;
        }
        return new DevConsoleRouteBuildItem("tests/printfailures", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                ts.get().printFullResults();
            }
        });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleRouteBuildItem toggleInstrumentation(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return null;
        }
        return new DevConsoleRouteBuildItem("tests/toggle-instrumentation", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                boolean instrumentationEnabled = ts.get().toggleInstrumentation();
                Json.JsonObjectBuilder object = Json.object();
                object.put("instrumentationEnabled", instrumentationEnabled);
                event.response().putHeader("Content-Type", "application/json; charset=utf-8").end(object.build());
            }
        });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleRouteBuildItem handleTestResult(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();
        if (testsDisabled(launchModeBuildItem, ts)) {
            return null;
        }

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
