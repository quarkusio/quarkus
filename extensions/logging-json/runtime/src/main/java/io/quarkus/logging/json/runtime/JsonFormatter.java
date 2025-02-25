package io.quarkus.logging.json.runtime;

import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.logmanager.ExtLogRecord;

public class JsonFormatter extends org.jboss.logmanager.formatters.JsonFormatter {

    private Set<String> excludedKeys;
    private Map<String, AdditionalField> additionalFields;

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

    @Override
    protected Generator createGenerator(final Writer writer) {
        Generator superGenerator = super.createGenerator(writer);
        return new FormatterJsonGenerator(superGenerator, this.excludedKeys);
    }

    @Override
    protected void after(final Generator generator, final ExtLogRecord record) throws Exception {
        for (var entry : this.additionalFields.entrySet()) {
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
