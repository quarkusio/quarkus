package io.quarkus.hibernate.orm.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RuntimeSettings {

    private final Map<String, Object> settings;

    private RuntimeSettings(Map<String, Object> settings) {
        this.settings = Collections.unmodifiableMap(new HashMap<>(settings));
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public Object get(String key) {
        return settings.get(key);
    }

    public boolean getBoolean(String key) {
        Object propertyValue = settings.get(key);
        return propertyValue != null && Boolean.parseBoolean(propertyValue.toString());
    }

    public boolean isConfigured(String key) {
        return settings.containsKey(key);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " {" + settings.toString() + "}";
    }

    public static class Builder {

        private final Map<String, Object> settings;

        public Builder(BuildTimeSettings buildTimeSettings, IntegrationSettings integrationSettings) {
            this.settings = new HashMap<>(buildTimeSettings.getSettings());
            this.settings.putAll(integrationSettings.getSettings());
        }

        public void put(String key, Object value) {
            settings.put(key, value);
        }

        public Object get(String key) {
            return settings.get(key);
        }

        public boolean isConfigured(String key) {
            return settings.containsKey(key);
        }

        public RuntimeSettings build() {
            return new RuntimeSettings(settings);
        }
    }
}
