package io.quarkus.produi.runtime.config;

import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class ConfigProdUIService {

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Get all configuration properties and their current values")
    public JsonArray getAllConfiguration() {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        TreeMap<String, JsonObject> sorted = new TreeMap<>();

        for (String name : config.getPropertyNames()) {
            if (name.isEmpty() || name.startsWith("%")) {
                continue;
            }
            try {
                var configValue = config.getConfigValue(name);
                if (configValue != null) {
                    JsonObject entry = new JsonObject();
                    entry.put("name", name);
                    entry.put("value", configValue.getValue() != null ? configValue.getValue() : "");
                    entry.put("source", configValue.getSourceName() != null ? configValue.getSourceName() : "");
                    sorted.put(name, entry);
                }
            } catch (Exception e) {
                // skip properties that can't be resolved
            }
        }

        // Also enumerate properties from all config sources for a more complete view
        for (var source : config.getConfigSources()) {
            for (String name : source.getPropertyNames()) {
                if (name.isEmpty() || name.startsWith("%") || sorted.containsKey(name)) {
                    continue;
                }
                try {
                    var configValue = config.getConfigValue(name);
                    if (configValue != null) {
                        JsonObject entry = new JsonObject();
                        entry.put("name", name);
                        entry.put("value", configValue.getValue() != null ? configValue.getValue() : "");
                        entry.put("source", configValue.getSourceName() != null ? configValue.getSourceName() : "");
                        sorted.put(name, entry);
                    }
                } catch (Exception e) {
                    // skip
                }
            }
        }

        JsonArray result = new JsonArray();
        sorted.values().forEach(result::add);
        return result;
    }

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Search configuration properties by name")
    public JsonArray searchConfig(String query) {
        JsonArray all = getAllConfiguration();
        if (query == null || query.isBlank()) {
            return all;
        }
        String lowerQuery = query.toLowerCase();
        JsonArray result = new JsonArray();
        for (int i = 0; i < all.size(); i++) {
            JsonObject entry = all.getJsonObject(i);
            if (entry.getString("name", "").toLowerCase().contains(lowerQuery)) {
                result.add(entry);
            }
        }
        return result;
    }
}
