package io.quarkus.logging.json.runtime;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Formatter;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logmanager.PropertyValues;
import org.jboss.logmanager.formatters.StructuredFormatter.Key;

import io.quarkus.logging.json.runtime.AdditionalFieldConfig.Type;
import io.quarkus.logging.json.runtime.JsonLogConfig.JsonConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LoggingJsonRecorder {

    public RuntimeValue<Optional<Formatter>> initializeConsoleJsonLogging(final JsonLogConfig config) {
        return getFormatter(config.consoleJson);
    }

    public RuntimeValue<Optional<Formatter>> initializeFileJsonLogging(final JsonLogConfig config) {
        return getFormatter(config.fileJson);
    }

    public RuntimeValue<Optional<Formatter>> initializeSyslogJsonLogging(JsonLogConfig config) {
        return getFormatter(config.syslogJson);
    }

    public RuntimeValue<Optional<Formatter>> initializeSocketJsonLogging(JsonLogConfig config) {
        return getFormatter(config.socketJson);
    }

    private RuntimeValue<Optional<Formatter>> getFormatter(JsonConfig config) {
        if (config.logFormat == JsonConfig.LogFormat.ECS) {
            addEcsFieldOverrides(config);
        }

        return getDefaultFormatter(config);
    }

    private RuntimeValue<Optional<Formatter>> getDefaultFormatter(JsonConfig config) {
        if (!config.enable) {
            return new RuntimeValue<>(Optional.empty());
        }
        final JsonFormatter formatter = config.keyOverrides.map(ko -> new JsonFormatter(ko)).orElse(new JsonFormatter());
        config.excludedKeys.ifPresent(ek -> formatter.setExcludedKeys(ek));
        Optional.ofNullable(config.additionalField).ifPresent(af -> formatter.setAdditionalFields(af));
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

    private void addEcsFieldOverrides(JsonConfig config) {
        EnumMap<Key, String> keyOverrides = PropertyValues.stringToEnumMap(Key.class, config.keyOverrides.orElse(null));
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
        config.keyOverrides = Optional.of(PropertyValues.mapToString(keyOverrides));

        config.additionalField.computeIfAbsent("ecs.version", k -> buildFieldConfig("1.12.2", Type.STRING));
        config.additionalField.computeIfAbsent("data_stream.type", k -> buildFieldConfig("logs", Type.STRING));

        Config quarkusConfig = ConfigProvider.getConfig();
        quarkusConfig.getOptionalValue("quarkus.application.name", String.class).ifPresent(
                s -> config.additionalField.computeIfAbsent("service.name", k -> buildFieldConfig(s, Type.STRING)));
        quarkusConfig.getOptionalValue("quarkus.application.version", String.class).ifPresent(
                s -> config.additionalField.computeIfAbsent("service.version", k -> buildFieldConfig(s, Type.STRING)));
        quarkusConfig.getOptionalValue("quarkus.profile", String.class).ifPresent(
                s -> config.additionalField.computeIfAbsent("service.environment", k -> buildFieldConfig(s, Type.STRING)));

        Set<String> excludedKeys = config.excludedKeys.orElseGet(HashSet::new);
        excludedKeys.add(Key.LOGGER_CLASS_NAME.getKey());
        excludedKeys.add(Key.RECORD.getKey());
        config.excludedKeys = Optional.of(excludedKeys);
    }

    private AdditionalFieldConfig buildFieldConfig(String value, Type type) {
        AdditionalFieldConfig field = new AdditionalFieldConfig();
        field.type = type;
        field.value = value;
        return field;
    }
}
