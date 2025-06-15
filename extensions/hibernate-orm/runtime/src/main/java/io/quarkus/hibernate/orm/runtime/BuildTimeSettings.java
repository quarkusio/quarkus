package io.quarkus.hibernate.orm.runtime;

import java.util.Map;

import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;

public class BuildTimeSettings {

    private RecordedConfig source;
    private Map<String, Object> quarkusConfigSettings;
    private Map<String, String> databaseOrmCompatibilitySettings;
    private Map<String, Object> allSettings;

    public BuildTimeSettings(RecordedConfig source, Map<String, Object> quarkusConfigSettings,
            Map<String, String> databaseOrmCompatibilitySettings, Map<String, Object> allSettings) {
        this.source = source;
        this.quarkusConfigSettings = Map.copyOf(quarkusConfigSettings);
        this.databaseOrmCompatibilitySettings = Map.copyOf(databaseOrmCompatibilitySettings);
        this.allSettings = Map.copyOf(allSettings);
    }

    public Object get(String key) {
        return allSettings.get(key);
    }

    public boolean getBoolean(String key) {
        Object propertyValue = allSettings.get(key);
        return propertyValue != null && Boolean.parseBoolean(propertyValue.toString());
    }

    public boolean isConfigured(String key) {
        return allSettings.containsKey(key);
    }

    public RecordedConfig getSource() {
        return source;
    }

    public Map<String, Object> getQuarkusConfigSettings() {
        return quarkusConfigSettings;
    }

    public Map<String, String> getDatabaseOrmCompatibilitySettings() {
        return databaseOrmCompatibilitySettings;
    }

    public Map<String, Object> getAllSettings() {
        return allSettings;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " {" + allSettings.toString() + "}";
    }
}
