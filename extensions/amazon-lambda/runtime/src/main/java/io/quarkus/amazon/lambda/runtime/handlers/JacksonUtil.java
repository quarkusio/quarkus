package io.quarkus.amazon.lambda.runtime.handlers;

import com.fasterxml.jackson.databind.JsonNode;

public class JacksonUtil {
    public static String getText(String name, JsonNode node) {
        JsonNode e = node.get(name);
        return e == null ? null : e.asText();
    }

    public static Long getLong(String name, JsonNode node) {
        JsonNode e = node.get(name);
        return e == null ? null : e.asLong();
    }
}
