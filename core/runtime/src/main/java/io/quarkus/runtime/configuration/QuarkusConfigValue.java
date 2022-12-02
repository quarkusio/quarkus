package io.quarkus.runtime.configuration;

import io.quarkus.runtime.ObjectSubstitution;
import io.smallrye.config.ConfigValue;

public class QuarkusConfigValue {
    private String name;
    private String value;
    private String rawValue;
    private String profile;
    private String configSourceName;
    private int configSourceOrdinal;
    private int configSourcePosition;
    private int lineNumber;

    public String getName() {
        return name;
    }

    public QuarkusConfigValue setName(final String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public QuarkusConfigValue setValue(final String value) {
        this.value = value;
        return this;
    }

    public String getRawValue() {
        return rawValue;
    }

    public QuarkusConfigValue setRawValue(final String rawValue) {
        this.rawValue = rawValue;
        return this;
    }

    public String getProfile() {
        return profile;
    }

    public QuarkusConfigValue setProfile(final String profile) {
        this.profile = profile;
        return this;
    }

    public String getConfigSourceName() {
        return configSourceName;
    }

    public QuarkusConfigValue setConfigSourceName(final String configSourceName) {
        this.configSourceName = configSourceName;
        return this;
    }

    public int getConfigSourceOrdinal() {
        return configSourceOrdinal;
    }

    public QuarkusConfigValue setConfigSourceOrdinal(final int configSourceOrdinal) {
        this.configSourceOrdinal = configSourceOrdinal;
        return this;
    }

    public int getConfigSourcePosition() {
        return configSourcePosition;
    }

    public QuarkusConfigValue setConfigSourcePosition(final int configSourcePosition) {
        this.configSourcePosition = configSourcePosition;
        return this;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public QuarkusConfigValue setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
        return this;
    }

    public static final class Substitution implements ObjectSubstitution<ConfigValue, QuarkusConfigValue> {
        @Override
        public QuarkusConfigValue serialize(final ConfigValue obj) {
            QuarkusConfigValue configValue = new QuarkusConfigValue();
            configValue.setName(obj.getName());
            configValue.setValue(obj.getValue());
            configValue.setRawValue(obj.getRawValue());
            configValue.setProfile(obj.getProfile());
            configValue.setConfigSourceName(obj.getConfigSourceName());
            configValue.setConfigSourceOrdinal(obj.getConfigSourceOrdinal());
            configValue.setConfigSourcePosition(obj.getConfigSourcePosition());
            configValue.setLineNumber(obj.getLineNumber());
            return configValue;
        }

        @Override
        public ConfigValue deserialize(final QuarkusConfigValue obj) {
            return ConfigValue.builder()
                    .withName(obj.getName())
                    .withValue(obj.getValue())
                    .withRawValue(obj.getRawValue())
                    .withProfile(obj.getProfile())
                    .withConfigSourceName(obj.getConfigSourceName())
                    .withConfigSourceOrdinal(obj.getConfigSourceOrdinal())
                    .withConfigSourcePosition(obj.getConfigSourcePosition())
                    .withLineNumber(obj.getLineNumber())
                    .build();
        }
    }
}
