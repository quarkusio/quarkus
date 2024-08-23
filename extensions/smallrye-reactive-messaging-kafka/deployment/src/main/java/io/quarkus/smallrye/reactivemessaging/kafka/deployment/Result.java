package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import java.util.HashMap;
import java.util.Map;

final class Result {
    // false when a result is known to not exist (unlike unknown, which is represented as null Result)
    final boolean exists;
    final String value; // serializer/deserializer type
    final Map<String, String> additionalProperties;

    static Result of(String result) {
        return new Result(true, result, new HashMap<>());
    }

    static Result nonexistent() {
        return new Result(false, null, new HashMap<>());
    }

    private Result(boolean exists, String value, Map<String, String> additionalProperties) {
        this.exists = exists;
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
        return new Result(this.exists, this.value, additionalProperties);
    }
}
