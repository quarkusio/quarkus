package io.quarkus.logging.json.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.NamedLogFormatsBuildItem;
import io.quarkus.logging.json.runtime.JsonLogConfig;
import io.quarkus.logging.json.runtime.LoggingJsonRecorder;

public final class LoggingJsonSteps {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public NamedLogFormatsBuildItem setUpFormatter(LoggingJsonRecorder recorder, JsonLogConfig config) {
        return new NamedLogFormatsBuildItem(
                recorder.initializeJsonLogging(config));
    }
}
