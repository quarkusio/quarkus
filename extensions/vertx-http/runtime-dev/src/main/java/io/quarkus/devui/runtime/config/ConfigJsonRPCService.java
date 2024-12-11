package io.quarkus.devui.runtime.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class ConfigJsonRPCService {

    @Inject
    ConfigDescriptionBean configDescriptionBean;

    public JsonArray getAllConfiguration() {
        return new JsonArray(configDescriptionBean.getAllConfig());
    }

    public JsonObject getAllValues() {
        JsonObject values = new JsonObject();
        for (ConfigDescription configDescription : configDescriptionBean.getAllConfig()) {
            values.put(configDescription.getName(), configDescription.getConfigValue().getValue());
        }
        return values;
    }

    public JsonObject getProjectProperties() {
        JsonObject response = new JsonObject();
        try {
            List<Path> resourcesDir = DevConsoleManager.getHotReplacementContext().getResourcesDir();
            if (resourcesDir.isEmpty()) {
                response.put("error", "Unable to manage configurations - no resource directory found");
            } else {

                // In the current project only
                Path path = resourcesDir.get(0);
                Path configPropertiesPath = path.resolve("application.properties");
                if (Files.exists(configPropertiesPath)) {
                    // Properties file
                    response.put("type", "properties");
                    String value = new String(Files.readAllBytes(configPropertiesPath));
                    response.put("value", value);
                } else {
                    response.put("type", "properties");
                    response.put("value", "");
                }
            }

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return response;
    }
}
