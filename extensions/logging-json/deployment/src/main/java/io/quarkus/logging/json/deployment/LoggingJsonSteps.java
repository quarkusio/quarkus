package io.quarkus.logging.json.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;
import io.quarkus.logging.json.runtime.JsonConfig;
import io.quarkus.logging.json.runtime.LoggingJsonRecorder;

public final class LoggingJsonSteps {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogConsoleFormatBuildItem setUpFormatter(LoggingJsonRecorder recorder, JsonConfig config) {
        return new LogConsoleFormatBuildItem(recorder.initializeJsonLogging(config));
    }
}
