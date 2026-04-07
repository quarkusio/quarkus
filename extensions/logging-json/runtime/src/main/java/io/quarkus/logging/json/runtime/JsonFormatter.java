package io.quarkus.logging.json.runtime;

import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.SPAN_ID;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.TRACE;
import static io.quarkus.logging.json.runtime.JsonFormatter.AdditionalKey.TRACE_SAMPLED;
import static io.quarkus.logging.json.runtime.JsonLogConfig.AdditionalFieldConfig.Type.STRING;
import static java.util.Optional.ofNullable;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.runtime.JsonLogConfig.JsonConfig.LogFormat;

public class JsonFormatter extends org.jboss.logmanager.formatters.JsonFormatter {

    private static final Logger LOG = Logger.getLogger(JsonFormatter.class);

    private Set<String> excludedKeys;
    private Map<String, AdditionalField> additionalFields;
    private LogFormat logFormat = LogFormat.DEFAULT;
    private String tracePrefix = "";
    private List<JsonProvider> discoveredProviders = Collections.emptyList();
    private volatile List<JsonProvider> jsonProviders;

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

    public void setDiscoveredProviders(List<JsonProvider> providers) {
        this.discoveredProviders = List.copyOf(providers);
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

        JsonLogGenerator jsonLogGenerator = new JsonLogGenerator(generator, this.excludedKeys);
        for (JsonProvider provider : getJsonProviders()) {
            provider.writeTo(jsonLogGenerator, record);
        }
    }

    private List<JsonProvider> getJsonProviders() {
        if (jsonProviders != null) {
            return jsonProviders;
        }
        List<JsonProvider> result = new ArrayList<>(discoveredProviders);
        try {
            CDI.current().select(JsonProvider.class).forEach(result::add);
            jsonProviders = Collections.unmodifiableList(result);
        } catch (Throwable ignored) {
            LOG.debug("CDI not available, JsonProvider CDI beans will not be loaded");
        }
        return result;
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

    /**
     * A JSON generator for use with {@link JsonProvider}.
     * Writes JSON fields for the current log record, respecting any keys configured via
     * {@code quarkus.log.*.json.excluded-keys}.
     */
    public static final class JsonLogGenerator {

        private final Generator delegate;
        private final Set<String> excludedKeys;
        private int skippedDepth = 0;

        JsonLogGenerator(final Generator delegate, final Set<String> excludedKeys) {
            this.delegate = delegate;
            this.excludedKeys = excludedKeys;
        }

        public JsonLogGenerator add(final String key, final boolean value) throws Exception {
            if (skippedDepth == 0 && !excludedKeys.contains(key)) {
                delegate.add(key, String.valueOf(value));
            }
            return this;
        }

        public JsonLogGenerator add(final String key, final int value) throws Exception {
            if (skippedDepth == 0 && !excludedKeys.contains(key)) {
                delegate.add(key, value);
            }
            return this;
        }

        public JsonLogGenerator add(final String key, final long value) throws Exception {
            if (skippedDepth == 0 && !excludedKeys.contains(key)) {
                delegate.add(key, value);
            }
            return this;
        }

        public JsonLogGenerator add(final String key, final Map<String, ?> value) throws Exception {
            if (skippedDepth == 0 && !excludedKeys.contains(key)) {
                delegate.add(key, value);
            }
            return this;
        }

        public JsonLogGenerator add(final String key, final String value) throws Exception {
            if (skippedDepth == 0 && !excludedKeys.contains(key)) {
                delegate.add(key, value);
            }
            return this;
        }

        public JsonLogGenerator startObject(final String key) throws Exception {
            if (skippedDepth > 0 || excludedKeys.contains(key)) {
                skippedDepth++;
            } else {
                delegate.startObject(key);
            }
            return this;
        }

        public JsonLogGenerator endObject() throws Exception {
            if (skippedDepth > 0) {
                skippedDepth--;
            } else {
                delegate.endObject();
            }
            return this;
        }

        public JsonLogGenerator startArray(final String key) throws Exception {
            if (skippedDepth > 0 || excludedKeys.contains(key)) {
                skippedDepth++;
            } else {
                delegate.startArray(key);
            }
            return this;
        }

        public JsonLogGenerator endArray() throws Exception {
            if (skippedDepth > 0) {
                skippedDepth--;
            } else {
                delegate.endArray();
            }
            return this;
        }
    }

    private static final class FormatterJsonGenerator implements Generator {

        private final Generator delegate;
        private final Set<String> excludedKeys;

        private FormatterJsonGenerator(final Generator delegate, final Set<String> excludedKeys) {
            this.delegate = delegate;
            this.excludedKeys = excludedKeys;
        }

        @Override
        public Generator begin() throws Exception {
            delegate.begin();
            return this;
        }

        @Override
        public Generator add(final String key, final int value) throws Exception {
            if (!excludedKeys.contains(key)) {
                delegate.add(key, value);
            }
            return this;
        }

        @Override
        public Generator add(final String key, final long value) throws Exception {
            if (!excludedKeys.contains(key)) {
                delegate.add(key, value);
            }
            return this;
        }

        @Override
        public Generator add(final String key, final Map<String, ?> value) throws Exception {
            if (!excludedKeys.contains(key)) {
                delegate.add(key, value);
            }
            return this;
        }

        @Override
        public Generator add(final String key, final String value) throws Exception {
            if (!excludedKeys.contains(key)) {
                delegate.add(key, value);
            }
            return this;
        }

        @Override
        public Generator startObject(final String key) throws Exception {
            delegate.startObject(key);
            return this;
        }

        @Override
        public Generator endObject() throws Exception {
            delegate.endObject();
            return this;
        }

        @Override
        public Generator startArray(final String key) throws Exception {
            delegate.startArray(key);
            return this;
        }

        @Override
        public Generator endArray() throws Exception {
            delegate.endArray();
            return this;
        }

        @Override
        public Generator end() throws Exception {
            delegate.end();
            return this;
        }
    }
}
