package io.quarkus.annotation.processor.documentation.config.model;

import java.util.Map;

/**
 * This is the enum accepted values that will appear in the documentation.
 */
public record EnumAcceptedValues(String qualifiedName, Map<String, EnumAcceptedValue> values) {

    public record EnumAcceptedValue(String configValue) {
    }
}
