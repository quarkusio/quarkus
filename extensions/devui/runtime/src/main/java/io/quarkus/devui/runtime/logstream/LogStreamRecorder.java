package io.quarkus.devui.runtime.logstream;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.logging.LogBuildTimeConfig;

@Recorder
public class LogStreamRecorder {
    private final LogBuildTimeConfig logBuildTimeConfig;

    public LogStreamRecorder(final LogBuildTimeConfig logBuildTimeConfig) {
        this.logBuildTimeConfig = logBuildTimeConfig;
    }

    public RuntimeValue<Optional<MutinyLogHandler>> mutinyLogHandler(String srcMainJava, List<String> knownClasses) {
        return new RuntimeValue<>(
                Optional.of(new MutinyLogHandler(logBuildTimeConfig.decorateStacktraces(), srcMainJava, knownClasses)));
    }
}
