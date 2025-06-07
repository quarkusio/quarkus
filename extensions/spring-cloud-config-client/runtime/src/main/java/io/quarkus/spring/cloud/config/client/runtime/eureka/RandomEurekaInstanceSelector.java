package io.quarkus.spring.cloud.config.client.runtime.eureka;

import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Random;

public class RandomEurekaInstanceSelector implements EurekaInstanceSelector {

    private static final Random random = new Random();

    @Override
    public JsonObject select(List<JsonObject> instances) {
        return instances.get(random.nextInt(instances.size()));
    }
}
