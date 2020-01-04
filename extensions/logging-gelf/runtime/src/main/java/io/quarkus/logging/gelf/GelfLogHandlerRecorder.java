package io.quarkus.logging.gelf;

import java.util.Map;
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
        handler.setFacility(config.facility);
        String extractStackTrace = String.valueOf(config.extractStackTrace);
        if (config.extractStackTrace && config.stackTraceThrowableReference != 0) {
            extractStackTrace = String.valueOf(config.stackTraceThrowableReference);
        }
        handler.setExtractStackTrace(extractStackTrace);
        handler.setFilterStackTrace(config.filterStackTrace);
        handler.setTimestampPattern(config.timestampPattern);
        handler.setIncludeFullMdc(config.includeFullMdc);
        handler.setHost(config.host);
        handler.setPort(config.port);
        handler.setLevel(config.level);

        // handle additional fields
        if (!config.additionalField.isEmpty()) {
            StringBuilder additionalFieldsValue = new StringBuilder();
            StringBuilder additionalFieldsType = new StringBuilder();
            for (Map.Entry<String, AdditionalFieldConfig> additionalField : config.additionalField.entrySet()) {
                if (additionalFieldsValue.length() > 0) {
                    additionalFieldsValue.append(',');
                }
                additionalFieldsValue.append(additionalField.getKey()).append('=').append(additionalField.getValue().value);

                if (additionalFieldsType.length() > 0) {
                    additionalFieldsType.append(',');
                }
                additionalFieldsType.append(additionalField.getKey()).append('=').append(additionalField.getValue().type);
            }

            handler.setAdditionalFields(additionalFieldsValue.toString());
            handler.setAdditionalFieldTypes(additionalFieldsType.toString());
        }

        return new RuntimeValue<>(Optional.of(handler));
    }
}
