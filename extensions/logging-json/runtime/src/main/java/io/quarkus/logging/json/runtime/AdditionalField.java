package io.quarkus.logging.json.runtime;

import io.quarkus.logging.json.runtime.JsonLogConfig.AdditionalFieldConfig;

public record AdditionalField(String value, AdditionalFieldConfig.Type type) {

}
