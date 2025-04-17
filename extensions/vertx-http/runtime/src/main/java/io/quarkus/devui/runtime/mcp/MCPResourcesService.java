package io.quarkus.devui.runtime.mcp;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * This expose all Dev UI BuildTimeData as Resources
 * We can problaby also add all RuntimeValue recorded JsonRPC Methods here
 * TODO: Implement this
 */
@ApplicationScoped
public class MCPResourcesService {

    public Map<String, List<Resource>> list() {
        return Map.of("resources", List.of());
    }

}