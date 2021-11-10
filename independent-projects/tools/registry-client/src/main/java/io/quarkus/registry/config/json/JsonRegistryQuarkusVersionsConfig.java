package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.registry.config.RegistryQuarkusVersionsConfig;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
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

    @Override
    public int hashCode() {
        return Objects.hash(exclusiveProvider, recognizedVersionsExpression);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonRegistryQuarkusVersionsConfig other = (JsonRegistryQuarkusVersionsConfig) obj;
        return exclusiveProvider == other.exclusiveProvider
                && Objects.equals(recognizedVersionsExpression, other.recognizedVersionsExpression);
    }
}
