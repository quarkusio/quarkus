package io.quarkus.logging.json.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;
import io.quarkus.deployment.builditem.LogFileFormatBuildItem;
import io.quarkus.deployment.builditem.LogSocketFormatBuildItem;
import io.quarkus.deployment.builditem.LogSyslogFormatBuildItem;
import io.quarkus.logging.json.runtime.JsonLogConfig;
import io.quarkus.logging.json.runtime.LoggingJsonRecorder;

public final class LoggingJsonProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogConsoleFormatBuildItem setUpConsoleFormatter(LoggingJsonRecorder recorder, JsonLogConfig config) {
        return new LogConsoleFormatBuildItem(recorder.initializeConsoleJsonLogging(config));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogFileFormatBuildItem setUpFileFormatter(LoggingJsonRecorder recorder, JsonLogConfig config) {
        return new LogFileFormatBuildItem(recorder.initializeFileJsonLogging(config));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogSyslogFormatBuildItem setUpSyslogFormatter(LoggingJsonRecorder recorder, JsonLogConfig config) {
        return new LogSyslogFormatBuildItem(recorder.initializeSyslogJsonLogging(config));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogSocketFormatBuildItem setUpSocketFormatter(LoggingJsonRecorder recorder, JsonLogConfig config) {
        return new LogSocketFormatBuildItem(recorder.initializeSocketJsonLogging(config));
    }
}
