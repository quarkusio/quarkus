package io.quarkus.logging.json.runtime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Formatter;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.JsonFormatter;
import org.jboss.logmanager.formatters.StructuredFormatter;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

@Recorder
public class LoggingJsonRecorder {
    public RuntimeValue<Optional<Map<String, Formatter>>> initializeJsonLogging(final JsonLogConfig config) {
        Map<String, Formatter> namedFormatterMap = new HashMap<>();
        if (config.console.enable) {
            namedFormatterMap.put("console", createJsonFormatter(config.console));
        }
        if (config.file.enable) {
            namedFormatterMap.put("file", createJsonFormatter(config.file));
        }
        if (config.syslog.enable) {
            namedFormatterMap.put("syslog", createJsonFormatter(config.syslog));
        }
        if (config.socket.enable) {
            namedFormatterMap.put("socket", createJsonFormatter(config.socket));
        }
        addNamedConfigs(config.consoleFormatters, namedFormatterMap);
        addNamedConfigs(config.fileFormatters, namedFormatterMap);
        addNamedConfigs(config.syslogFormatters, namedFormatterMap);
        addNamedConfigs(config.socketFormatters, namedFormatterMap);
        return new RuntimeValue<>(Optional.of(namedFormatterMap));
    }

    private void addNamedConfigs(Map<String, NamedHandlerJsonConfig> namedConfigs, Map<String, Formatter> namedFormatterMap) {
        for (Map.Entry<String, NamedHandlerJsonConfig> entry : namedConfigs.entrySet()) {
            if (entry.getValue().jsonConfig.enable) {
                String name = entry.getKey();
                NamedHandlerJsonConfig config = entry.getValue();
                if (namedFormatterMap.containsKey(name)) {
                    throw new RuntimeException(String.format("Only one formatter can be configured with the same name '%s'",
                            name));
                }
                namedFormatterMap.put(name, createJsonFormatter(config.jsonConfig));
            }
        }

    }

    private static JsonFormatter createJsonFormatter(JsonConfig config) {
        String keyOverrides = config.keyoverrides;
        Map<StructuredFormatter.Key, String> keyOverrideMap = null;
        if (!keyOverrides.equals("<NA>")) {
            keyOverrideMap = new HashMap<>();
            for (String pair : keyOverrides.split(",")) {
                String[] split = pair.split("=", 2);
                if (split.length == 2) {
                    StructuredFormatter.Key key = translateToKey(split[0]);
                    if (key != null) {
                        keyOverrideMap.put(key, split[1]);
                    }
                } else {
                    throw new ConfigurationException(
                            "Key override pair '" + pair + "' is invalid, key and value should be separated by = character");
                }
            }
        }
        JsonFormatter formatter;
        Map<String, JsonConfig.AdditionalFieldConfig> additionalField = config.additionalField;
        if (!additionalField.isEmpty()) {
            Map<String, String> additionalFields = new HashMap<>();
            for (Map.Entry<String, JsonConfig.AdditionalFieldConfig> field : additionalField.entrySet()) {
                additionalFields.put(field.getKey(), field.getValue().value);
            }
            formatter = new CustomFieldsJsonFormatter(keyOverrideMap, additionalFields);
        } else
            formatter = new JsonFormatter(keyOverrideMap);
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
        return formatter;
    }

    private static StructuredFormatter.Key translateToKey(String name) {
        try {
            return StructuredFormatter.Key.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(
                    "Invalid key: " + name + ". Valid values are: " + Arrays.toString(StructuredFormatter.Key.values()));
        }
    }

    public static class CustomFieldsJsonFormatter extends JsonFormatter {
        public Map<String, String> additionalFields;

        public CustomFieldsJsonFormatter(Map<Key, String> keyOverrides, Map<String, String> additionalFields) {
            super(keyOverrides);
            this.additionalFields = additionalFields;
        }

        @Override
        protected void after(Generator generator, ExtLogRecord record) throws Exception {
            super.after(generator, record);
            if (!additionalFields.isEmpty()) {
                for (Map.Entry<String, String> entry : additionalFields.entrySet())
                    generator.add(entry.getKey(), entry.getValue());
            }
        }

    }
}
