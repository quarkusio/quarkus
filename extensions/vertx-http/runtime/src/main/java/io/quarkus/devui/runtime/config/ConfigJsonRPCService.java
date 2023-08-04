package io.quarkus.devui.runtime.config;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.runtime.comms.JsonRpcMessage;
import io.quarkus.devui.runtime.comms.MessageType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class ConfigJsonRPCService {
    private static final Logger LOG = Logger.getLogger(ConfigJsonRPCService.class.getName());

    @Inject
    ConfigDescriptionBean configDescriptionBean;

    public JsonRpcMessage<Boolean> updateProperty(String name, String value) {
        DevConsoleManager.invoke("config-update-property", Map.of("name", name, "value", value));
        return new JsonRpcMessage(true, MessageType.HotReload);
    }

    public boolean updateProperties(String content, String type) {
        if (type.equalsIgnoreCase("properties")) {
            Properties p = new Properties();
            try (StringReader sr = new StringReader(content)) {
                p.load(sr); // Validate
                Map<String, String> m = Map.of("content", content, "type", type);
                DevConsoleManager.invoke("config-set-properties", m);
                return true;
            } catch (IOException ex) {
                LOG.error("Could not update properties", ex);
                return false;
            }
        }
        return false;
    }

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

    private Map<String, String> toMap(Properties prop) {
        return prop.entrySet().stream().collect(
                Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next, HashMap::new));
    }
}
