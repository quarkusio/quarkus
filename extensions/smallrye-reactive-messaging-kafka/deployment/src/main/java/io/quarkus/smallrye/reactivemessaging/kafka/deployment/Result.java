package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import java.util.HashMap;
import java.util.Map;

final class Result {
    final String value; // serializer/deserializer type
    final Map<String, String> additionalProperties;

    static Result of(String result) {
        return new Result(result, new HashMap<>());
    }

    private Result(String value, Map<String, String> additionalProperties) {
        this.value = value;
        this.additionalProperties = additionalProperties;
    }

    Result with(String key, String value) {
        return with(true, key, value);
    }

    Result with(boolean condition, String key, String value) {
        if (!condition) {
            return this;
        }

        Map<String, String> additionalProperties = new HashMap<>(this.additionalProperties);
        additionalProperties.put(key, value);
        return new Result(this.value, additionalProperties);
    }
}
