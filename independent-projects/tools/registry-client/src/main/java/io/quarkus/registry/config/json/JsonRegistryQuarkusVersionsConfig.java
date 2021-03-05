package io.quarkus.registry.config.json;

import io.quarkus.registry.config.RegistryQuarkusVersionsConfig;

public class JsonRegistryQuarkusVersionsConfig implements RegistryQuarkusVersionsConfig {

    private String recognizedVersionsExpression;
    private boolean exclusiveProvider;

    @Override
    public String getRecognizedVersionsExpression() {
        return recognizedVersionsExpression;
    }

    public void setRecognizedVersionsExpression(String recognizedVersionsExpression) {
        this.recognizedVersionsExpression = recognizedVersionsExpression;
    }

    @Override
    public boolean isExclusiveProvider() {
        return exclusiveProvider;
    }

    public void setExclusiveProvider(boolean exclusiveProvider) {
        this.exclusiveProvider = exclusiveProvider;
    }
}
