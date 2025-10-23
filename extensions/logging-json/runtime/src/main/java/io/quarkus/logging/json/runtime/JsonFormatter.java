package io.quarkus.logging.json.runtime;

import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.SPAN_ID;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.TRACE;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.TRACE_SAMPLED;
import static io.quarkus.logging.json.runtime.JsonLogConfig.AdditionalFieldConfig.Type.STRING;
import static java.util.Optional.ofNullable;

import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.runtime.JsonLogConfig.JsonConfig.LogFormat;

public class JsonFormatter extends org.jboss.logmanager.formatters.JsonFormatter {

    private Set<String> excludedKeys;
    private Map<String, AdditionalField> additionalFields;
    private LogFormat logFormat = LogFormat.DEFAULT;
    private String tracePrefix = "";

    public enum AdditionalKey {
        ECS_VERSION("ecs.version"),
        DATA_STREAM_TYPE("data_stream.type"),
        SERVICE_NAME("service.name"),
        SERVICE_VERSION("service.version"),
        SERVICE_ENVIRONMENT("service.environment"),
        TRACE("trace"),
        SPAN_ID("spanId"),
        TRACE_SAMPLED("traceSampled");

        private final String key;

        AdditionalKey(final String key) {
            this.key = key;
        }

        /**
         * Returns the name of the key for the structure.
         *
         * @return the name of they key
         */
        public String getKey() {
            return key;
        }
    }

    /**
     * Creates a new JSON formatter.
     *
     */
    public JsonFormatter() {
        super();
        this.excludedKeys = new HashSet<>();
        this.additionalFields = new HashMap<>();
    }

    /**
     * Creates a new JSON formatter.
     *
     * @param keyOverrides a string representation of a map to override keys
     *
     *        "@see org.jboss.logmanager.ext.PropertyValues#stringToEnumMap(Class, String)"
     */
    public JsonFormatter(final String keyOverrides) {
        super(keyOverrides);
        this.excludedKeys = new HashSet<>();
        this.additionalFields = new HashMap<>();
    }

    /**
     * Creates a new JSON formatter.
     *
     * @param keyOverrides a string representation of a map to override keys
     *
     *        "@see org.jboss.logmanager.ext.PropertyValues#stringToEnumMap(Class, String)"
     * @param excludedKeys a list of keys to be excluded when writing the output
     * @param additionalFields additionalFields to be added to the output
     */
    public JsonFormatter(final String keyOverrides, final Set<String> excludedKeys,
            final Map<String, AdditionalField> additionalFields) {
        super(keyOverrides);
        this.excludedKeys = excludedKeys;
        this.additionalFields = additionalFields;
    }

    public Set<String> getExcludedKeys() {
        return this.excludedKeys;
    }

    public void setExcludedKeys(Set<String> excludedKeys) {
        this.excludedKeys = excludedKeys;
    }

    public Map<String, AdditionalField> getAdditionalFields() {
        return this.additionalFields;
    }

    public void setAdditionalFields(Map<String, AdditionalField> additionalFields) {
        this.additionalFields = additionalFields;
    }

    public void setLogFormat(LogFormat logFormat) {
        this.logFormat = logFormat;
    }

    public void setTracePrefix(String tracePrefix) {
        this.tracePrefix = tracePrefix;
    }

    @Override
    protected Generator createGenerator(final Writer writer) {
        Generator superGenerator = super.createGenerator(writer);
        return new FormatterJsonGenerator(superGenerator, this.excludedKeys);
    }

    @Override
    protected void after(final Generator generator, final ExtLogRecord record) throws Exception {

        if (logFormat.equals(LogFormat.GCP)) {
            final Map<String, String> mdcCopy = record.getMdcCopy();
            if (!mdcCopy.isEmpty()) {
                Map<String, AdditionalField> current = new HashMap<>(additionalFields);
                current.computeIfPresent(TRACE.getKey(), (key, value) -> {
                    final String traceId = mdcCopy.get("traceId");
                    if (traceId != null && !traceId.isEmpty()) {
                        return new AdditionalField(tracePrefix + traceId, STRING);
                    } else {
                        return value;
                    }
                });
                current.computeIfPresent(SPAN_ID.getKey(),
                        (key, value) -> new AdditionalField(ofNullable(mdcCopy.get("spanId")).orElse(""), STRING));
                current.computeIfPresent(TRACE_SAMPLED.getKey(),
                        (key, value) -> new AdditionalField(ofNullable(mdcCopy.get("sampled")).orElse(""), STRING));

                addToGenerator(current, generator);
            } else {
                // fast path
                addToGenerator(additionalFields, generator);
            }
        } else {
            // fast path
            addToGenerator(additionalFields, generator);
        }
    }

    private void addToGenerator(Map<String, AdditionalField> fields, Generator generator) throws Exception {
        for (var entry : fields.entrySet()) {
            switch (entry.getValue().type()) {
                case STRING:
                    generator.add(entry.getKey(), entry.getValue().value());
                    break;
                case INT:
                    generator.add(entry.getKey(), Integer.valueOf(entry.getValue().value()));
                    break;
                case LONG:
                    generator.add(entry.getKey(), Long.valueOf(entry.getValue().value()));
                    break;
            }
        }
    }

    private static class FormatterJsonGenerator implements Generator {
        private final Generator generator;
        private final Set<String> excludedKeys;

        private FormatterJsonGenerator(final Generator generator, final Set<String> excludedKeys) {
            this.generator = generator;
            this.excludedKeys = excludedKeys;
        }

        @Override
        public Generator begin() throws Exception {
            generator.begin();
            return this;
        }

        @Override
        public Generator add(final String key, final int value) throws Exception {
            if (!excludedKeys.contains(key)) {
                generator.add(key, value);
            }
            return this;
        }

        @Override
        public Generator add(final String key, final long value) throws Exception {
            if (!excludedKeys.contains(key)) {
                generator.add(key, value);
            }
            return this;
        }

        @Override
        public Generator add(final String key, final Map<String, ?> value) throws Exception {
            if (!excludedKeys.contains(key)) {
                generator.add(key, value);
            }
            return this;
        }

        @Override
        public Generator add(final String key, final String value) throws Exception {
            if (!excludedKeys.contains(key)) {
                generator.add(key, value);
            }
            return this;
        }

        @Override
        public Generator startObject(final String key) throws Exception {
            generator.startObject(key);
            return this;
        }

        @Override
        public Generator endObject() throws Exception {
            generator.endObject();
            return this;
        }

        @Override
        public Generator startArray(final String key) throws Exception {
            generator.startArray(key);
            return this;
        }

        @Override
        public Generator endArray() throws Exception {
            generator.endArray();
            return this;
        }

        @Override
        public Generator end() throws Exception {
            generator.end();
            return this;
        }
    }
}
