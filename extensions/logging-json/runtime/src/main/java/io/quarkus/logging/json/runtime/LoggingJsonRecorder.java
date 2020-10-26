package io.quarkus.logging.json.runtime;

import java.util.Collections;
import java.util.Optional;
import java.util.logging.Formatter;

import org.jboss.logmanager.formatters.JsonFormatter;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logmanager.formatters.StructuredFormatter;

@Recorder
public class LoggingJsonRecorder {
    public RuntimeValue<Optional<Formatter>> initializeJsonLogging(final JsonConfig config) {
        if (!config.enable) {
            return new RuntimeValue<>(Optional.empty());
        }
        final JsonFormatter formatter = config.levelNameOverride
                .map(this::levelOverridingFormatter)
                .orElseGet(JsonFormatter::new);
        formatter.setPrettyPrint(config.prettyPrint);
        final String dateFormat = config.dateFormat;
        if (!dateFormat.equals("default")) {
            formatter.setDateFormat(dateFormat);
        }
        formatter.setExceptionOutputType(config.exceptionOutputType);
        formatter.setPrintDetails(config.printDetails);
        config.recordDelimiter.ifPresent(formatter::setRecordDelimiter);
        final String zoneId = config.zoneId;
        if (!zoneId.equals("default")) {
            formatter.setZoneId(zoneId);
        }
        return new RuntimeValue<>(Optional.of(formatter));
    }

    private JsonFormatter levelOverridingFormatter(String levelNameOverride) {
        return new JsonFormatter(Collections.singletonMap(StructuredFormatter.Key.LEVEL, levelNameOverride));
    }
}
