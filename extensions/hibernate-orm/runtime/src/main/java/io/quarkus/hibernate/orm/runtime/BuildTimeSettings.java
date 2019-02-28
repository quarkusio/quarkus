package io.quarkus.hibernate.orm.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BuildTimeSettings {

    private Map<String, Object> settings;

    public BuildTimeSettings(Map<String, Object> settings) {
        this.settings = Collections.unmodifiableMap(new HashMap<>(settings));
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

    public Map<String, Object> getSettings() {
        return settings;
    }

}
