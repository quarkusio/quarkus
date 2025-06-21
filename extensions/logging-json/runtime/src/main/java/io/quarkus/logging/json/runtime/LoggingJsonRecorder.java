package io.quarkus.logging.json.runtime;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logmanager.PropertyValues;
import org.jboss.logmanager.formatters.StructuredFormatter.Key;

import io.quarkus.logging.json.runtime.JsonLogConfig.AdditionalFieldConfig.Type;
import io.quarkus.logging.json.runtime.JsonLogConfig.JsonConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LoggingJsonRecorder {
    private final RuntimeValue<JsonLogConfig> runtimeConfig;

    public LoggingJsonRecorder(final RuntimeValue<JsonLogConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public RuntimeValue<Optional<Formatter>> initializeConsoleJsonLogging() {
        return getFormatter(runtimeConfig.getValue().consoleJson());
    }

    public RuntimeValue<Optional<Formatter>> initializeFileJsonLogging() {
        return getFormatter(runtimeConfig.getValue().fileJson());
    }

    public RuntimeValue<Optional<Formatter>> initializeSyslogJsonLogging() {
        return getFormatter(runtimeConfig.getValue().syslogJson());
    }

    public RuntimeValue<Optional<Formatter>> initializeSocketJsonLogging() {
        return getFormatter(runtimeConfig.getValue().socketJson());
    }

    private RuntimeValue<Optional<Formatter>> getFormatter(JsonConfig config) {
        String keyOverrides = config.keyOverrides().orElse(null);
        Set<String> excludedKeys = config.excludedKeys().orElse(Set.of());
        Map<String, AdditionalField> additionalFields = config.additionalField().entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> new AdditionalField(e.getValue().value(), e.getValue().type()),
                        (x, y) -> y, LinkedHashMap::new));

        OverridableJsonConfig overridableJsonConfig = new OverridableJsonConfig(keyOverrides, excludedKeys, additionalFields);

        if (config.logFormat() == JsonConfig.LogFormat.ECS) {
            overridableJsonConfig = addEcsFieldOverrides(overridableJsonConfig);
        }

        return getDefaultFormatter(config, overridableJsonConfig);
    }

    private RuntimeValue<Optional<Formatter>> getDefaultFormatter(JsonConfig config,
            OverridableJsonConfig overridableJsonConfig) {
        if (!config.enabled().orElse(config.enable())) {
            return new RuntimeValue<>(Optional.empty());
        }

        final JsonFormatter formatter;
        if (overridableJsonConfig.keyOverrides() == null) {
            formatter = new JsonFormatter();
        } else {
            formatter = new JsonFormatter(overridableJsonConfig.keyOverrides());
        }
        formatter.setExcludedKeys(overridableJsonConfig.excludedKeys());
        formatter.setAdditionalFields(overridableJsonConfig.additionalFields());
        formatter.setPrettyPrint(config.prettyPrint());
        final String dateFormat = config.dateFormat();
        if (!dateFormat.equals("default")) {
            formatter.setDateFormat(dateFormat);
        }
        formatter.setExceptionOutputType(config.exceptionOutputType());
        formatter.setPrintDetails(config.printDetails());
        config.recordDelimiter().ifPresent(formatter::setRecordDelimiter);
        final String zoneId = config.zoneId();
        if (!zoneId.equals("default")) {
            formatter.setZoneId(zoneId);
        }
        return new RuntimeValue<>(Optional.of(formatter));
    }

    private OverridableJsonConfig addEcsFieldOverrides(OverridableJsonConfig overridableJsonConfig) {
        EnumMap<Key, String> keyOverrides = PropertyValues.stringToEnumMap(Key.class, overridableJsonConfig.keyOverrides());
        keyOverrides.putIfAbsent(Key.TIMESTAMP, "@timestamp");
        keyOverrides.putIfAbsent(Key.LOGGER_NAME, "log.logger");
        keyOverrides.putIfAbsent(Key.LEVEL, "log.level");
        keyOverrides.putIfAbsent(Key.PROCESS_ID, "process.pid");
        keyOverrides.putIfAbsent(Key.PROCESS_NAME, "process.name");
        keyOverrides.putIfAbsent(Key.THREAD_NAME, "process.thread.name");
        keyOverrides.putIfAbsent(Key.THREAD_ID, "process.thread.id");
        keyOverrides.putIfAbsent(Key.HOST_NAME, "host.hostname");
        keyOverrides.putIfAbsent(Key.SEQUENCE, "event.sequence");
        keyOverrides.putIfAbsent(Key.EXCEPTION_MESSAGE, "error.message");
        keyOverrides.putIfAbsent(Key.STACK_TRACE, "error.stack_trace");

        Set<String> excludedKeys = new HashSet<>(overridableJsonConfig.excludedKeys());
        excludedKeys.add(Key.LOGGER_CLASS_NAME.getKey());
        excludedKeys.add(Key.RECORD.getKey());

        Map<String, AdditionalField> additionalFields = new LinkedHashMap<>(overridableJsonConfig.additionalFields());
        additionalFields.computeIfAbsent("ecs.version", k -> new AdditionalField("1.12.2", Type.STRING));
        additionalFields.computeIfAbsent("data_stream.type", k -> new AdditionalField("logs", Type.STRING));

        Config quarkusConfig = ConfigProvider.getConfig();
        quarkusConfig.getOptionalValue("quarkus.application.name", String.class).ifPresent(
                s -> additionalFields.computeIfAbsent("service.name", k -> new AdditionalField(s, Type.STRING)));
        quarkusConfig.getOptionalValue("quarkus.application.version", String.class).ifPresent(
                s -> additionalFields.computeIfAbsent("service.version", k -> new AdditionalField(s, Type.STRING)));
        quarkusConfig.getOptionalValue("quarkus.profile", String.class).ifPresent(
                s -> additionalFields.computeIfAbsent("service.environment", k -> new AdditionalField(s, Type.STRING)));

        return new OverridableJsonConfig(PropertyValues.mapToString(keyOverrides), excludedKeys, additionalFields);
    }

    private record OverridableJsonConfig(String keyOverrides, Set<String> excludedKeys,
            Map<String, AdditionalField> additionalFields) {
    }
}
