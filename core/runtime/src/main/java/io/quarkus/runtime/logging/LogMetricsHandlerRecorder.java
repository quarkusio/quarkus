package io.quarkus.runtime.logging;

import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.logging.Handler;

import org.jboss.logmanager.Level;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.metrics.MetricsFactory;

@Recorder
public class LogMetricsHandlerRecorder {

    static final String METRIC_NAME = "log.total";

    static final String METRIC_DESCRIPTION = "Number of log events, per log level. Non-standard levels are counted with the lower standard level.";

    static final List<Level> STANDARD_LEVELS = Arrays.asList(Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG,
            Level.TRACE);

    static final NavigableMap<Integer, LongAdder> COUNTERS = new TreeMap<>();

    public void initCounters() {
        for (Level level : STANDARD_LEVELS) {
            LongAdder counter = new LongAdder();
            // Use integer value to match any non-standard equivalent level
            COUNTERS.put(level.intValue(), counter);
        }
    }

    public Consumer<MetricsFactory> registerMetrics() {
        return new Consumer<MetricsFactory>() {
            @Override
            public void accept(MetricsFactory metricsFactory) {
                for (Level level : STANDARD_LEVELS) {
                    metricsFactory.builder(METRIC_NAME).description(METRIC_DESCRIPTION).tag("level", level.getName())
                            .buildCounter(COUNTERS.get(level.intValue())::sum);
                }
            }
        };
    }

    public RuntimeValue<Optional<Handler>> getLogHandler() {
        return new RuntimeValue(Optional.of(new LogMetricsHandler(COUNTERS)));
    }
}
