package io.quarkus.spring.cloud.config.client.runtime.eureka;

import java.util.List;
import java.util.Random;

import io.vertx.core.json.JsonObject;

public class RandomEurekaInstanceSelector implements EurekaInstanceSelector {

    private final Random random = new Random();

    @Override
    public JsonObject select(List<JsonObject> instances) {
        return instances.get(random.nextInt(instances.size()));
    }
}
