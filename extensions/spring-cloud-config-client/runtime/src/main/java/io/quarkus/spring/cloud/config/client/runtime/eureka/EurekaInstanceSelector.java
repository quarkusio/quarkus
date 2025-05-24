package io.quarkus.spring.cloud.config.client.runtime.eureka;

import java.util.List;

import io.vertx.core.json.JsonObject;

@FunctionalInterface
public interface EurekaInstanceSelector {
    JsonObject select(List<JsonObject> instances);
}
