package io.quarkus.deployment.dev.assistant;

import java.util.Map;

public record DynamicOutput(Map<String, Object> json) {

    @Override
    public String toString() {
        return json.toString(); // We don't have a Object->Json lib available here. Maybe move to vertx-http ?
    }

}
