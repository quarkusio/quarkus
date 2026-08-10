package io.quarkus.produi.runtime.config;

import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.produi.api.SecretMasker;
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
                JsonObject entry = toEntry(config, name);
                if (entry != null) {
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
                    JsonObject entry = toEntry(config, name);
                    if (entry != null) {
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

    private JsonObject toEntry(SmallRyeConfig config, String name) {
        var configValue = config.getConfigValue(name);
        if (configValue == null) {
            return null;
        }
        String value = configValue.getValue() != null ? configValue.getValue() : "";
        boolean secret = SecretMasker.isSecret(name, value);
        JsonObject entry = new JsonObject();
        entry.put("name", name);
        entry.put("value", SecretMasker.maskIfSecret(name, value));
        entry.put("source", configValue.getSourceName() != null ? configValue.getSourceName() : "");
        if (secret) {
            entry.put("secret", true);
        }
        return entry;
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
