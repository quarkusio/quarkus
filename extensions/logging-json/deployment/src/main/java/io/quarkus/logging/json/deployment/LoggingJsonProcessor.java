package io.quarkus.logging.json.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;
import io.quarkus.deployment.builditem.LogFileFormatBuildItem;
import io.quarkus.deployment.builditem.LogSocketFormatBuildItem;
import io.quarkus.deployment.builditem.LogSyslogFormatBuildItem;
import io.quarkus.logging.json.runtime.LoggingJsonRecorder;

public final class LoggingJsonProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogConsoleFormatBuildItem setUpConsoleFormatter(LoggingJsonRecorder recorder) {
        return new LogConsoleFormatBuildItem(recorder.initializeConsoleJsonLogging());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogFileFormatBuildItem setUpFileFormatter(LoggingJsonRecorder recorder) {
        return new LogFileFormatBuildItem(recorder.initializeFileJsonLogging());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogSyslogFormatBuildItem setUpSyslogFormatter(LoggingJsonRecorder recorder) {
        return new LogSyslogFormatBuildItem(recorder.initializeSyslogJsonLogging());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogSocketFormatBuildItem setUpSocketFormatter(LoggingJsonRecorder recorder) {
        return new LogSocketFormatBuildItem(recorder.initializeSocketJsonLogging());
    }
}
