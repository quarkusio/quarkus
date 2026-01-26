package io.quarkus.bootstrap.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A simple JSON string generator.
 */
public final class Json {

    private static final String OBJECT_START = "{";
    private static final String OBJECT_END = "}";
    private static final String ARRAY_START = "[";
    private static final String ARRAY_END = "]";
    private static final String NAME_VAL_SEPARATOR = ":";
    private static final String ENTRY_SEPARATOR = ",";

    private static final int CONTROL_CHAR_START = 0;
    private static final int CONTROL_CHAR_END = 0x1f;
    private static final char CHAR_QUOTATION_MARK = '"';

    private static final Map<Character, String> REPLACEMENTS;

    static {
        REPLACEMENTS = new HashMap<>();
        // control characters
        for (int i = CONTROL_CHAR_START; i <= CONTROL_CHAR_END; i++) {
            REPLACEMENTS.put((char) i, String.format("\\u%04x", i));
        }
        // quotation mark
        REPLACEMENTS.put('"', "\\\"");
        // reverse solidus
        REPLACEMENTS.put('\\', "\\\\");
    }

    private Json() {
    }

    /**
     * @return the new JSON array builder, empty builders are not ignored
     */
    public static JsonArrayBuilder array() {
        return new JsonArrayBuilder(false);
    }

    /**
     * @param initialCapacity initial underlying array capacity
     * @return the new JSON array builder, empty builders are not ignored
     */
    public static JsonArrayBuilder array(int initialCapacity) {
        return new JsonArrayBuilder(false, initialCapacity);
    }

    /**
     * @param ignoreEmptyBuilders
     * @return the new JSON array builder
     * @see JsonBuilder#ignoreEmptyBuilders
     */
    public static JsonArrayBuilder array(boolean ignoreEmptyBuilders) {
        return new JsonArrayBuilder(ignoreEmptyBuilders);
    }

    /**
     * @param ignoreEmptyBuilders
     * @param initialCapacity initial underlying array capacity
     * @return the new JSON array builder
     * @see JsonBuilder#ignoreEmptyBuilders
     */
    public static JsonArrayBuilder array(boolean ignoreEmptyBuilders, int initialCapacity) {
        return new JsonArrayBuilder(ignoreEmptyBuilders, initialCapacity);
    }

    /**
     * @return the new JSON object builder, empty builders are not ignored
     */
    public static JsonObjectBuilder object() {
        return new JsonObjectBuilder(false);
    }

    /**
     * @param initialCapacity initial underlying map capacity
     * @return the new JSON object builder, empty builders are not ignored
     */
    public static JsonObjectBuilder object(int initialCapacity) {
        return new JsonObjectBuilder(false, initialCapacity);
    }

    /**
     * @param ignoreEmptyBuilders
     * @return the new JSON object builder
     * @see JsonBuilder#ignoreEmptyBuilders
     */
    public static JsonObjectBuilder object(boolean ignoreEmptyBuilders) {
        return new JsonObjectBuilder(ignoreEmptyBuilders);
    }

    /**
     * @param ignoreEmptyBuilders
     * @param initialCapacity initial underlying map capacity
     * @return the new JSON object builder
     * @see JsonBuilder#ignoreEmptyBuilders
     */
    public static JsonObjectBuilder object(boolean ignoreEmptyBuilders, int initialCapacity) {
        return new JsonObjectBuilder(ignoreEmptyBuilders, initialCapacity);
    }

    public abstract static class JsonBuilder<T> {

        protected final boolean ignoreEmptyBuilders;
        protected JsonTransform transform;

        /**
         * @param ignoreEmptyBuilders If set to true all empty builders added to this builder will be ignored during
         *        {@link #appendTo(Appendable)}
         */
        JsonBuilder(boolean ignoreEmptyBuilders) {
            this.ignoreEmptyBuilders = ignoreEmptyBuilders;
        }

        /**
         * @return <code>true</code> if there are no elements/properties, <code>false</code> otherwise
         */
        abstract boolean isEmpty();

        public abstract void appendTo(Appendable appendable) throws IOException;

        /**
         * @param value value to check
         * @return <code>true</code> if the value is null or an empty builder and {@link #ignoreEmptyBuilders} is set to
         *         <code>true</code>, <code>false</code>
         *         otherwise
         */
        protected boolean isIgnored(Object value) {
            return value == null
                    || (ignoreEmptyBuilders && value instanceof JsonBuilder<?> jsonBuilder && jsonBuilder.isEmpty());
        }

        protected boolean isValuesEmpty(Collection<Object> values) {
            if (values.isEmpty()) {
                return true;
            }
            for (Object object : values) {
                if (!(object instanceof JsonBuilder<?> jsonBuilder) || !jsonBuilder.isEmpty()) {
                    return false;
                }
            }
            return true;

        }

        protected abstract T self();

        abstract void add(JsonValue element);

        void setTransform(JsonTransform transform) {
            this.transform = transform;
        }

        public void transform(JsonMultiValue value, JsonTransform transform) {
            final ResolvedTransform resolved = new ResolvedTransform(this, transform);
            value.forEach(resolved);
        }
    }

    /**
     * JSON array builder.
     */
    public static class JsonArrayBuilder extends JsonBuilder<JsonArrayBuilder> implements Collection<Object> {

        private final List<Object> values;

        private JsonArrayBuilder(boolean ignoreEmptyBuilders) {
            super(ignoreEmptyBuilders);
            this.values = new ArrayList<>();
        }

        private JsonArrayBuilder(boolean ignoreEmptyBuilders, int initialCapacity) {
            super(ignoreEmptyBuilders);
            this.values = new ArrayList<>(initialCapacity);
        }

        public JsonArrayBuilder add(JsonArrayBuilder value) {
            addInternal(value);
            return this;
        }

        public JsonArrayBuilder add(JsonObjectBuilder value) {
            addInternal(value);
            return this;
        }

        public JsonArrayBuilder add(String value) {
            addInternal(value);
            return this;
        }

        public JsonArrayBuilder add(boolean value) {
            addInternal(value);
            return this;
        }

        public JsonArrayBuilder add(int value) {
            addInternal(value);
            return this;
        }

        public JsonArrayBuilder add(long value) {
            addInternal(value);
            return this;
        }

        public JsonArrayBuilder addAll(List<JsonObjectBuilder> value) {
            if (value != null && !value.isEmpty()) {
                values.addAll(value);
            }
            return this;
        }

        void addInternal(Object value) {
            if (value != null) {
                values.add(value);
            }
        }

        @Override
        public int size() {
            return values.size();
        }

        public boolean isEmpty() {
            return isValuesEmpty(values);
        }

        @Override
        public boolean contains(Object o) {
            return values.contains(o);
        }

        @Override
        public Iterator<Object> iterator() {
            return values.iterator();
        }

        @Override
        public Object[] toArray() {
            return values.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return values.toArray(a);
        }

        @Override
        public boolean add(Object o) {
            addInternal(o);
            return o != null;
        }

        @Override
        public boolean remove(Object o) {
            return values.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return values.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<?> c) {
            for (Object o : c) {
                addInternal(o);
            }
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return values.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return values.retainAll(c);
        }

        @Override
        public void clear() {
            values.clear();
        }

        @Override
        public void appendTo(Appendable appendable) throws IOException {
            appendable.append(ARRAY_START);
            int idx = 0;
            for (Object value : values) {
                if (isIgnored(value)) {
                    continue;
                }
                if (++idx > 1) {
                    appendable.append(ENTRY_SEPARATOR);
                }
                appendValue(appendable, value);
            }
            appendable.append(ARRAY_END);
        }

        @Override
        protected JsonArrayBuilder self() {
            return this;
        }

        @Override
        void add(JsonValue element) {
            if (element instanceof JsonString jsonStr) {
                add(jsonStr.value());
            } else if (element instanceof JsonInteger jsonInt) {
                final long longValue = jsonInt.longValue();
                final int intValue = (int) longValue;
                if (longValue == intValue) {
                    add(intValue);
                } else {
                    add(longValue);
                }
            } else if (element instanceof JsonBoolean jsonBoolean) {
                add(jsonBoolean.value());
            } else if (element instanceof JsonArray jsonArray) {
                final JsonArrayBuilder arrayBuilder = Json.array(ignoreEmptyBuilders);
                arrayBuilder.transform(jsonArray, transform);
                if (!arrayBuilder.isEmpty()) {
                    add(arrayBuilder);
                }
            } else if (element instanceof JsonObject jsonObj) {
                final JsonObjectBuilder objectBuilder = Json.object(ignoreEmptyBuilders);
                objectBuilder.transform(jsonObj, transform);
                if (!objectBuilder.isEmpty()) {
                    add(objectBuilder);
                }
            }
        }
    }

    /**
     * JSON object builder.
     */
    public static class JsonObjectBuilder extends JsonBuilder<JsonObjectBuilder> implements Map<String, Object> {

        private final Map<String, Object> properties;

        private JsonObjectBuilder(boolean ignoreEmptyBuilders) {
            super(ignoreEmptyBuilders);
            this.properties = new HashMap<>();
        }

        private JsonObjectBuilder(boolean ignoreEmptyBuilders, int initialCapacity) {
            super(ignoreEmptyBuilders);
            this.properties = new HashMap<>(initialCapacity);
        }

        public JsonObjectBuilder put(String name, String value) {
            putInternal(name, value);
            return this;
        }

        public JsonObjectBuilder put(String name, JsonObjectBuilder value) {
            putInternal(name, value);
            return this;
        }

        public JsonObjectBuilder put(String name, JsonArrayBuilder value) {
            putInternal(name, value);
            return this;
        }

        public JsonObjectBuilder put(String name, boolean value) {
            putInternal(name, value);
            return this;
        }

        public JsonObjectBuilder put(String name, int value) {
            putInternal(name, value);
            return this;
        }

        public JsonObjectBuilder put(String name, long value) {
            putInternal(name, value);
            return this;
        }

        public boolean has(String name) {
            return properties.containsKey(name);
        }

        Object putInternal(String name, Object value) {
            Objects.requireNonNull(name);
            if (value != null) {
                return properties.put(name, value);
            }
            return null;
        }

        @Override
        public int size() {
            return properties.size();
        }

        public boolean isEmpty() {
            if (properties.isEmpty()) {
                return true;
            }
            return isValuesEmpty(properties.values());
        }

        @Override
        public boolean containsKey(Object key) {
            return properties.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return properties.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            return properties.get(key);
        }

        @Override
        public Object put(String key, Object value) {
            return putInternal(key, value);
        }

        @Override
        public Object remove(Object key) {
            return properties.remove(key);
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            for (Entry<? extends String, ?> entry : m.entrySet()) {
                putInternal(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void clear() {
            properties.clear();
        }

        @Override
        public Set<String> keySet() {
            return properties.keySet();
        }

        @Override
        public Collection<Object> values() {
            return properties.values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return properties.entrySet();
        }

        @Override
        public void appendTo(Appendable appendable) throws IOException {
            appendable.append(OBJECT_START);
            int idx = 0;
            for (Entry<String, Object> entry : properties.entrySet()) {
                if (isIgnored(entry.getValue())) {
                    continue;
                }
                if (++idx > 1) {
                    appendable.append(ENTRY_SEPARATOR);
                }
                appendStringValue(appendable, entry.getKey());
                appendable.append(NAME_VAL_SEPARATOR);
                appendValue(appendable, entry.getValue());
            }
            appendable.append(OBJECT_END);
        }

        @Override
        protected JsonObjectBuilder self() {
            return this;
        }

        @Override
        void add(JsonValue element) {
            if (element instanceof JsonMember member) {
                final String attribute = member.attribute().value();
                final JsonValue value = member.value();
                if (value instanceof JsonString jsonStr) {
                    put(attribute, jsonStr.value());
                } else if (value instanceof JsonInteger jsonInt) {
                    final long longValue = jsonInt.longValue();
                    final int intValue = (int) longValue;
                    if (longValue == intValue) {
                        put(attribute, intValue);
                    } else {
                        put(attribute, longValue);
                    }
                } else if (value instanceof JsonBoolean jsonBool) {
                    put(attribute, jsonBool.value());
                } else if (value instanceof JsonArray jsonArr) {
                    final JsonArrayBuilder arrayBuilder = Json.array(ignoreEmptyBuilders);
                    arrayBuilder.transform(jsonArr, transform);
                    if (!arrayBuilder.isEmpty()) {
                        put(attribute, arrayBuilder);
                    }
                } else if (value instanceof JsonObject jsonObj) {
                    final JsonObjectBuilder objectBuilder = Json.object(ignoreEmptyBuilders);
                    objectBuilder.transform(jsonObj, transform);
                    if (!objectBuilder.isEmpty()) {
                        put(attribute, objectBuilder);
                    }
                }
            }
        }
    }

    static void appendValue(Appendable appendable, Object value) throws IOException {
        if (value instanceof JsonObjectBuilder jsonObj) {
            jsonObj.appendTo(appendable);
        } else if (value instanceof JsonArrayBuilder jsonArr) {
            jsonArr.appendTo(appendable);
        } else if (value instanceof String str) {
            appendStringValue(appendable, str);
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            appendable.append(value.toString());
        } else {
            throw new IllegalStateException("Unsupported value type: " + value);
        }
    }

    static void appendStringValue(Appendable appendable, String value) throws IOException {
        appendable.append(CHAR_QUOTATION_MARK);
        appendEscaped(appendable, value);
        appendable.append(CHAR_QUOTATION_MARK);
    }

    /**
     * Escape quotation mark, reverse solidus and control characters (U+0000 through U+001F).
     *
     * @param value value to escape
     * @see <a href="https://www.ietf.org/rfc/rfc4627.txt">https://www.ietf.org/rfc/rfc4627.txt</a>
     */
    static void appendEscaped(Appendable appendable, String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            final String replacement = REPLACEMENTS.get(c);
            if (replacement != null) {
                appendable.append(replacement);
            } else {
                appendable.append(c);
            }
        }
    }

    private static final class ResolvedTransform implements JsonTransform {
        private final JsonBuilder<?> resolvedBuilder;
        private final JsonTransform transform;

        private ResolvedTransform(JsonBuilder<?> resolvedBuilder, JsonTransform transform) {
            this.resolvedBuilder = resolvedBuilder;
            this.resolvedBuilder.setTransform(transform);
            this.transform = transform;
        }

        @Override
        public void accept(JsonBuilder<?> builder, JsonValue element) {
            if (builder == null) {
                transform.accept(resolvedBuilder, element);
            }
        }
    }
}
