package io.quarkus.hibernate.orm.runtime;

import java.util.Map;

import io.quarkus.hibernate.orm.runtime.config.DatabaseOrmCompatibilityVersion;

public class BuildTimeSettings {

    private Map<String, Object> quarkusConfigSettings;
    private DatabaseOrmCompatibilityVersion databaseOrmCompatibilityVersion;
    private Map<String, String> databaseOrmCompatibilitySettings;
    private Map<String, Object> allSettings;

    public BuildTimeSettings(Map<String, Object> quarkusConfigSettings,
            DatabaseOrmCompatibilityVersion databaseOrmCompatibilityVersion,
            Map<String, String> databaseOrmCompatibilitySettings,
            Map<String, Object> allSettings) {
        this.quarkusConfigSettings = Map.copyOf(quarkusConfigSettings);
        this.databaseOrmCompatibilityVersion = databaseOrmCompatibilityVersion;
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

    public Map<String, Object> getQuarkusConfigSettings() {
        return quarkusConfigSettings;
    }

    public DatabaseOrmCompatibilityVersion getDatabaseOrmCompatibilityVersion() {
        return databaseOrmCompatibilityVersion;
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
