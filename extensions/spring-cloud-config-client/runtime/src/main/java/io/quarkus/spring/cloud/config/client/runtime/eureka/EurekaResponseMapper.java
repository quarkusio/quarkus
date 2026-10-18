package io.quarkus.spring.cloud.config.client.runtime.eureka;

import java.util.List;

import org.jboss.logging.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class EurekaResponseMapper {

    private static final Logger log = Logger.getLogger(EurekaResponseMapper.class);

    public List<JsonObject> instances(String jsonString) {
        JsonObject json = new JsonObject(jsonString);
        log.debug("Received response from Spring Cloud Config Server: " + json.encodePrettily());
        JsonArray jsonArray = json.getJsonObject("application").getJsonArray("instance");
        return jsonArray.stream()
                .map(o -> (JsonObject) o)
                .toList();
    }
}
