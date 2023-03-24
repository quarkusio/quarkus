package io.quarkus.devui.runtime.config;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.dev.console.DevConsoleManager;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class ConfigJsonRpcService {

    public boolean updateProperty(String name, String value) {
        DevConsoleManager.invoke("config-update-property", Map.of("name", name, "value", value));
        return true;
    }

    public JsonObject getAllValues() {
        JsonObject values = new JsonObject();
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            values.put(name, config.getConfigValue(name).getValue());
        }
        return values;
    }
}
