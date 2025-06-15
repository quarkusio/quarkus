package io.quarkus.spring.cloud.config.client.runtime.eureka;

import io.vertx.core.json.JsonObject;

import java.util.List;

@FunctionalInterface
public interface EurekaInstanceSelector {
    JsonObject select(List<JsonObject> instances);
}
