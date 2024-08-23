package io.quarkus.logging.gelf;

import static biz.paluch.logging.RuntimeContainerProperties.PROPERTY_LOGSTASH_GELF_SKIP_HOSTNAME_RESOLUTION;

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

        String previousSkipHostnameResolution = null;
        if (config.skipHostnameResolution) {
            previousSkipHostnameResolution = System.setProperty(PROPERTY_LOGSTASH_GELF_SKIP_HOSTNAME_RESOLUTION, "true");
        }
        final JBoss7GelfLogHandler handler = new JBoss7GelfLogHandler();
        if (config.skipHostnameResolution) {
            if (previousSkipHostnameResolution == null) {
                System.clearProperty(PROPERTY_LOGSTASH_GELF_SKIP_HOSTNAME_RESOLUTION);
            } else {
                System.setProperty(PROPERTY_LOGSTASH_GELF_SKIP_HOSTNAME_RESOLUTION, previousSkipHostnameResolution);
            }
        }
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
        handler.setDynamicMdcFields(config.dynamicMdcFields.orElse(null));
        handler.setMdcFields(config.mdcFields.orElse(null));
        handler.setDynamicMdcFieldTypes(config.dynamicMdcFieldTypes.orElse(null));
        handler.setHost(config.host);
        handler.setPort(config.port);
        handler.setLevel(config.level);
        handler.setMaximumMessageSize(config.maximumMessageSize);
        handler.setIncludeLocation(config.includeLocation);
        handler.setIncludeLogMessageParameters(config.includeLogMessageParameters);
        if (config.originHost.isPresent()) {
            handler.setOriginHost(config.originHost.get());
        }

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
