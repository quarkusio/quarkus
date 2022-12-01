package io.quarkus.hibernate.orm.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IntegrationSettings {

    private final Map<String, Object> settings;

    private IntegrationSettings(Map<String, Object> settings) {
        this.settings = Collections.unmodifiableMap(new HashMap<>(settings));
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public static class Builder {

        private final Map<String, Object> settings = new HashMap<>();

        public Builder() {
        }

        public void put(String key, Object value) {
            settings.put(key, value);
        }

        public IntegrationSettings build() {
            return new IntegrationSettings(settings);
        }
    }
}
