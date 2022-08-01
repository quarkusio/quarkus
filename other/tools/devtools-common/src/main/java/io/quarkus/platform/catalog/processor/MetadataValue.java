package io.quarkus.platform.catalog.processor;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MetadataValue {
    private static final MetadataValue EMPTY_METADATA_VALUE = new MetadataValue(null);
    private final Object val;

    public MetadataValue(Object val) {
        this.val = val;
    }

    public static MetadataValue get(Map<String, Object> data, String path) {
        final Map<String, Object> safeData = data != null ? data : Collections.emptyMap();
        if (!path.contains(".")) {
            return new MetadataValue(safeData.get(path));
        }
        int index = path.indexOf(".");
        String key = path.substring(0, index);
        if (safeData.get(key) instanceof Map) {
            return get((Map<String, Object>) safeData.get(key), path.substring(index + 1));
        } else {
            return EMPTY_METADATA_VALUE;
        }
    }

    public boolean isEmpty() {
        return val == null;
    }

    public String asString() {
        if (val instanceof String) {
            return (String) val;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<String> asStringList() {
        if (val instanceof String) {
            return Collections.singletonList((String) val);
        } else if (val instanceof List && !((List<?>) val).isEmpty()
                && ((List<?>) val).get(0) instanceof String) {
            return (List<String>) val;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public boolean asBoolean() {
        if (val == null) {
            return false;
        }
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return false;
    }

    public <T extends Enum<T>> T toEnum(Class<T> clazz) {
        return toEnum(clazz, null);
    }

    public <T extends Enum<T>> T toEnum(Class<T> clazz, T defaultValue) {
        final String name = asString();
        if (name == null) {
            return defaultValue;
        }
        try {
            return T.valueOf(clazz, name.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

}
