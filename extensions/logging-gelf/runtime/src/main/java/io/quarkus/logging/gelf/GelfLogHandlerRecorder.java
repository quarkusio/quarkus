package io.quarkus.logging.gelf;

import java.util.Optional;
import java.util.logging.Handler;

import biz.paluch.logging.gelf.jboss7.JBoss7GelfLogHandler;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GelfLogHandlerRecorder {
    public RuntimeValue<Optional<Handler>> initializeHandler(final GelfConfig config) {
        if (!config.enabled) {
            return new RuntimeValue<>(Optional.empty());
        }

        final JBoss7GelfLogHandler handler = new JBoss7GelfLogHandler();
        handler.setVersion(config.version);
        handler.setFacility("jboss-logmanager");
        String extractStackTrace = String.valueOf(config.extractStackTrace);
        if (config.extractStackTrace && config.stackTraceThrowableReference != 0) {
            extractStackTrace = String.valueOf(config.stackTraceThrowableReference);
        }
        handler.setExtractStackTrace(extractStackTrace);
        handler.setFilterStackTrace(config.filterStackTrace);
        handler.setTimestampPattern(config.timestampPattern);
        handler.setHost(config.host);
        handler.setPort(config.port);
        return new RuntimeValue<>(Optional.of(handler));
    }
}
