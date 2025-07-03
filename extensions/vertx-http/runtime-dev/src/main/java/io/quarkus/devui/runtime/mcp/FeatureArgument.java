package io.quarkus.devui.runtime.mcp;

import io.vertx.core.json.JsonObject;

record FeatureArgument(String name,
        String description,
        boolean required,
        java.lang.reflect.Type type,
        String defaultValue,
        Provider provider) {

    JsonObject asJson() {
        return new JsonObject()
                .put("name", name)
                .put("description", description)
                .put("required", required);
    }

    boolean isParam() {
        return provider == Provider.PARAMS;
    }

    enum Provider {
        PARAMS,
        REQUEST_ID,
        REQUEST_URI,
        MCP_CONNECTION,
        MCP_LOG,
        PROGRESS,
        ROOTS,
        SAMPLING,
    }
}
