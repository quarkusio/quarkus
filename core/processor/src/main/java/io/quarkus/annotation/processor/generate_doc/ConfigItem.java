package io.quarkus.annotation.processor.generate_doc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final public class ConfigItem {
    private static final Map<String, String> PRIMITIVE_DEFAULT_VALUES = new HashMap<>();

    static {
        PRIMITIVE_DEFAULT_VALUES.put("int", "0");
        PRIMITIVE_DEFAULT_VALUES.put("long", "0l");
        PRIMITIVE_DEFAULT_VALUES.put("float", "0f");
        PRIMITIVE_DEFAULT_VALUES.put("double", "0d");
        PRIMITIVE_DEFAULT_VALUES.put("boolean", "false");
    }

    private final String type;
    private final String javaDocKey;
    private final String key;
    private final String defaultValue;
    private final ConfigPhase configPhase;

    public ConfigItem(String key, String javaDocKey, String type, String defaultValue,
            ConfigPhase configPhase) {
        this.type = type;
        this.javaDocKey = javaDocKey;
        this.key = key;
        this.defaultValue = defaultValue;
        this.configPhase = configPhase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConfigItem that = (ConfigItem) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(javaDocKey, that.javaDocKey) &&
                Objects.equals(key, that.key) &&
                Objects.equals(defaultValue, that.defaultValue) &&
                Objects.equals(configPhase, that.configPhase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, javaDocKey, key, defaultValue, configPhase);
    }

    @Override
    public String toString() {
        return "ConfigItem{" +
                "type='" + type + '\'' +
                ", key='" + key + '\'' +
                ", javaDocKey='" + javaDocKey + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", configPhase='" + configPhase + '\'' +
                '}';
    }

    public String getType() {
        return type;
    }

    public String getJavaDocKey() {
        return javaDocKey;
    }

    public String getPropertyName() {
        return key;
    }

    public String getDefaultValue() {
        if (!defaultValue.isEmpty()) {
            return defaultValue;
        }

        return PRIMITIVE_DEFAULT_VALUES.containsKey(type) ? PRIMITIVE_DEFAULT_VALUES.get(type) : "";
    }

    public ConfigPhase getConfigPhase() {
        return configPhase;
    }
}
