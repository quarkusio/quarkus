package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.registry.config.RegistryQuarkusVersionsConfig;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonRegistryQuarkusVersionsConfig implements RegistryQuarkusVersionsConfig.Mutable {

    private String recognizedVersionsExpression;
    private boolean exclusiveProvider;

    @Override
    public String getRecognizedVersionsExpression() {
        return recognizedVersionsExpression;
    }

    public Mutable setRecognizedVersionsExpression(String recognizedVersionsExpression) {
        this.recognizedVersionsExpression = recognizedVersionsExpression;
        return this;
    }

    @Override
    public boolean isExclusiveProvider() {
        return exclusiveProvider;
    }

    public Mutable setExclusiveProvider(boolean exclusiveProvider) {
        this.exclusiveProvider = exclusiveProvider;
        return this;
    }

    @Override
    public Mutable mutable() {
        return new JsonRegistryQuarkusVersionsConfig()
                .setExclusiveProvider(this.exclusiveProvider)
                .setRecognizedVersionsExpression(this.recognizedVersionsExpression);
    }

    @Override
    public RegistryQuarkusVersionsConfig build() {
        return this;
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
