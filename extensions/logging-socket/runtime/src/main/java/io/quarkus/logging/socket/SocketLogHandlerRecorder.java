package io.quarkus.logging.socket;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Formatter;
import java.util.logging.Handler;

import org.jboss.logmanager.Logger;
import org.jboss.logmanager.formatters.JsonFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.formatters.StructuredFormatter;
import org.jboss.logmanager.handlers.SocketHandler;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

@Recorder
public class SocketLogHandlerRecorder {
    private static final Logger LOG = Logger.getLogger(SocketLogHandlerRecorder.class.getName());

    public RuntimeValue<Optional<Handler>> initializeHandler(final SocketConfig config) {
        if (!config.enabled) {
            return new RuntimeValue<>(Optional.empty());
        }
        try {
            SocketHandler handler = new SocketHandler(config.protocol, config.host, config.port);
            handler.setLevel(config.level);
            Formatter formatter = createFormatter(config);
            handler.setFormatter(formatter);
            handler.setBlockOnReconnect(config.blockOnReconnect);
            handler.setAutoFlush(true);
            return new RuntimeValue<>(Optional.of(handler));
        } catch (UnknownHostException e) {
            throw new ConfigurationException(
                    "Socket logging can not resolve host", e);
        }
    }

    private Formatter createFormatter(SocketConfig config) {
        switch (config.formatter) {
            case "json":
                return createJsonFormatter(config);
            case "pattern":
                return new PatternFormatter(config.patternFormat);
            default:
                throw new ConfigurationException("Unexpected formmatter type, expected are: json, pattern");
        }
    }

    private JsonFormatter createJsonFormatter(SocketConfig config) {
        String keyoverrides = config.keyoverrides;
        Map<StructuredFormatter.Key, String> keyOverrideMap = null;
        if (keyoverrides != null && !keyoverrides.isBlank() && !keyoverrides.equals("<NA>")) {
            keyOverrideMap = new HashMap<>();
            for (String pair : keyoverrides.split(",")) {
                String[] split = pair.split("=", 2);
                if (split.length == 2) {
                    StructuredFormatter.Key key = translateToKey(split[0]);
                    keyOverrideMap.put(key, split[1]);
                } else {
                    throw new ConfigurationException(
                            "Key override pair '" + pair + "' is invalid, key and value should be separated by = character");
                }
            }
        }
        if (config.additionalField != null) {
            Map<String, String> additionalFields = new HashMap<>();
            for (Map.Entry<String, AdditionalFieldConfig> field : config.additionalField.entrySet()) {
                additionalFields.put(field.getKey(), field.getValue().value);
            }
            return new AdvancedJsonFormatter(keyOverrideMap, additionalFields);
        }
        return new JsonFormatter(keyOverrideMap);
    }

    private StructuredFormatter.Key translateToKey(String name) {
        try {
            return StructuredFormatter.Key.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(
                    "Invalid key: " + name + ". Valid values are: " + Arrays.toString(StructuredFormatter.Key.values()));
        }
    }
}
