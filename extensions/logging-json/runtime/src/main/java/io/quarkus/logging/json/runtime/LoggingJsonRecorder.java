package io.quarkus.logging.json.runtime;

import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.DATA_STREAM_TYPE;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.ECS_VERSION;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.SERVICE_ENVIRONMENT;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.SERVICE_NAME;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.SERVICE_VERSION;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.SPAN_ID;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.TRACE;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.TRACE_SAMPLED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jboss.logmanager.PropertyValues;
import org.jboss.logmanager.formatters.StructuredFormatter.Key;

import io.quarkus.logging.json.runtime.JsonLogConfig.AdditionalFieldConfig.Type;
import io.quarkus.logging.json.runtime.JsonLogConfig.JsonConfig;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.logging.NamedHandlerType;

@Recorder
public class LoggingJsonRecorder {

    private static final Logger LOG = Logger.getLogger(LoggingJsonRecorder.class);

    private final RuntimeValue<JsonLogConfig> runtimeConfig;
    private final RuntimeValue<ApplicationConfig> applicationConfig;
    private List<JsonProvider> jsonProviders = Collections.emptyList();

    public LoggingJsonRecorder(final RuntimeValue<JsonLogConfig> runtimeConfig,
            final RuntimeValue<ApplicationConfig> applicationConfig) {
        this.runtimeConfig = runtimeConfig;
        this.applicationConfig = applicationConfig;
    }

    public RuntimeValue<Optional<Formatter>> initializeConsoleJsonLogging() {
        return getFormatter(runtimeConfig.getValue().consoleJson(), jsonProviders);
    }

    public RuntimeValue<Optional<Formatter>> initializeFileJsonLogging() {
        return getFormatter(runtimeConfig.getValue().fileJson(), jsonProviders);
    }

    public RuntimeValue<Optional<Formatter>> initializeSyslogJsonLogging() {
        return getFormatter(runtimeConfig.getValue().syslogJson(), jsonProviders);
    }

    public RuntimeValue<Optional<Formatter>> initializeSocketJsonLogging() {
        return getFormatter(runtimeConfig.getValue().socketJson(), jsonProviders);
    }

    public RuntimeValue<Map<NamedHandlerType, Map<String, Optional<Formatter>>>> initializeNamedJsonLogging() {
        JsonLogConfig config = runtimeConfig.getValue();
        Map<NamedHandlerType, Map<String, Optional<Formatter>>> result = new EnumMap<>(NamedHandlerType.class);
        result.put(NamedHandlerType.CONSOLE, collectFormatters(config.jsonConsoleHandlers(), jsonProviders));
        result.put(NamedHandlerType.FILE, collectFormatters(config.jsonFileHandlers(), jsonProviders));
        result.put(NamedHandlerType.SYSLOG, collectFormatters(config.jsonSyslogHandlers(), jsonProviders));
        result.put(NamedHandlerType.SOCKET, collectFormatters(config.jsonSocketHandlers(), jsonProviders));
        return new RuntimeValue<>(result);
    }

    public void initializeJsonProviders(List<String> classNames) {
        jsonProviders = instantiateProviders(classNames);
    }

    private List<JsonProvider> instantiateProviders(List<String> classNames) {
        List<JsonProvider> providers = new ArrayList<>(classNames.size());
        for (String className : classNames) {
            try {
                providers.add((JsonProvider) Class.forName(className, true,
                        Thread.currentThread().getContextClassLoader()).getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                LOG.warnf(e, "Failed to instantiate JsonProvider: %s", className);
            }
        }
        return providers;
    }

    private Map<String, Optional<Formatter>> collectFormatters(
            Map<String, ? extends JsonLogConfig.JsonHandlerConfig> handlers, List<JsonProvider> providers) {
        Map<String, Optional<Formatter>> formatters = new HashMap<>();
        handlers.forEach((name, config) -> {
            if (config.json().enabled().isPresent()) {
                formatters.put(name, getFormatter(config.json(), providers).getValue());
            }
        });
        return formatters;
    }

    private RuntimeValue<Optional<Formatter>> getFormatter(JsonConfig config, List<JsonProvider> providers) {
        String keyOverrides = config.keyOverrides().orElse(null);
        Set<String> excludedKeys = config.excludedKeys().orElse(Set.of());
        Map<String, AdditionalField> additionalFields = config.additionalField().entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> new AdditionalField(e.getValue().value(), e.getValue().type()),
                        (x, y) -> y, LinkedHashMap::new));

        OverridableJsonConfig overridableJsonConfig = new OverridableJsonConfig(keyOverrides, excludedKeys, additionalFields);

        if (config.logFormat() == JsonConfig.LogFormat.ECS) {
            overridableJsonConfig = addEcsFieldOverrides(overridableJsonConfig);
        } else if (config.logFormat() == JsonConfig.LogFormat.GCP) {
            overridableJsonConfig = addGCPFieldOverrides(overridableJsonConfig);
        }

        return getDefaultFormatter(config, overridableJsonConfig, providers);
    }

    private RuntimeValue<Optional<Formatter>> getDefaultFormatter(JsonConfig config,
            OverridableJsonConfig overridableJsonConfig, List<JsonProvider> providers) {
        if (!config.enabled().orElse(config.enable())) {
            return new RuntimeValue<>(Optional.empty());
        }

        final JsonFormatter formatter;
        if (overridableJsonConfig.keyOverrides() == null) {
            formatter = new JsonFormatter();
        } else {
            formatter = new JsonFormatter(overridableJsonConfig.keyOverrides());
        }
        formatter.setLogFormat(config.logFormat());
        if (JsonConfig.LogFormat.GCP == config.logFormat()) {
            formatter.setTracePrefix("projects/" + applicationConfig.getValue().name().orElse("") + "/traces/");
        }
        formatter.setExcludedKeys(overridableJsonConfig.excludedKeys());
        formatter.setAdditionalFields(overridableJsonConfig.additionalFields());
        formatter.setDiscoveredProviders(providers);
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
        additionalFields.computeIfAbsent(ECS_VERSION.getKey(), k -> new AdditionalField("1.12.2", Type.STRING));
        additionalFields.computeIfAbsent(DATA_STREAM_TYPE.getKey(), k -> new AdditionalField("logs", Type.STRING));

        Config quarkusConfig = ConfigProvider.getConfig();
        quarkusConfig.getOptionalValue("quarkus.application.name", String.class).ifPresent(
                s -> additionalFields.computeIfAbsent(SERVICE_NAME.getKey(), k -> new AdditionalField(s, Type.STRING)));
        quarkusConfig.getOptionalValue("quarkus.application.version", String.class).ifPresent(
                s -> additionalFields.computeIfAbsent(SERVICE_VERSION.getKey(), k -> new AdditionalField(s, Type.STRING)));
        quarkusConfig.getOptionalValue("quarkus.profile", String.class).ifPresent(
                s -> additionalFields.computeIfAbsent(SERVICE_ENVIRONMENT.getKey(), k -> new AdditionalField(s, Type.STRING)));

        return new OverridableJsonConfig(PropertyValues.mapToString(keyOverrides), excludedKeys, additionalFields);
    }

    private OverridableJsonConfig addGCPFieldOverrides(OverridableJsonConfig overridableJsonConfig) {
        EnumMap<Key, String> keyOverrides = PropertyValues.stringToEnumMap(Key.class, overridableJsonConfig.keyOverrides());
        keyOverrides.putIfAbsent(Key.LEVEL, "severity");

        Set<String> excludedKeys = new HashSet<>(overridableJsonConfig.excludedKeys());
        Map<String, AdditionalField> additionalFields = new LinkedHashMap<>(overridableJsonConfig.additionalFields());
        // data comes from the MDC context and is only available in the JsonFormater
        additionalFields.computeIfAbsent(TRACE.getKey(), k -> new AdditionalField("", Type.STRING));
        additionalFields.computeIfAbsent(SPAN_ID.getKey(), k -> new AdditionalField("", Type.STRING));
        additionalFields.computeIfAbsent(TRACE_SAMPLED.getKey(), k -> new AdditionalField("", Type.STRING));

        return new OverridableJsonConfig(PropertyValues.mapToString(keyOverrides), excludedKeys, additionalFields);
    }

    private record OverridableJsonConfig(String keyOverrides, Set<String> excludedKeys,
            Map<String, AdditionalField> additionalFields) {
    }
}
