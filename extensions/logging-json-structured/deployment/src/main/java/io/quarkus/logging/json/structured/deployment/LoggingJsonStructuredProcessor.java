package io.quarkus.logging.json.structured.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;
import io.quarkus.logging.json.structured.JsonStructuredConfig;
import io.quarkus.logging.json.structured.LoggingJsonStructuredRecorder;

class LoggingJsonStructuredProcessor {

    private static final String FEATURE = "logging-json-structured";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogConsoleFormatBuildItem setUpFormatter(LoggingJsonStructuredRecorder recorder, JsonStructuredConfig config) {
        return new LogConsoleFormatBuildItem(recorder.initializeJsonLogging(config));
    }
}
