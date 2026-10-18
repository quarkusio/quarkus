package io.quarkus.devui.runtime.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class ConfigJsonRPCService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;

    @Inject
    ConfigDescriptionBean configDescriptionBean;

    public JsonArray getAllConfiguration() {
        return new JsonArray(configDescriptionBean.getAllConfig());
    }

    @JsonRpcDescription("Get all configurations and their values for the Quarkus application")
    @JsonRpcUsage({ Usage.DEV_UI })
    public JsonObject getAllValues() {
        JsonObject values = new JsonObject();
        for (ConfigDescription configDescription : configDescriptionBean.getAllConfig()) {
            values.put(configDescription.getName(), configDescription.getConfigValue().getValue());
        }
        return values;
    }

    @JsonRpcDescription("Search for configuration keys in the Quarkus application by matching against key names and descriptions. Returns matching configuration properties with their full metadata including description, type, default value, current value, phase and allowed values.")
    @DevMCPEnableByDefault
    public JsonArray searchConfig(
            @JsonRpcDescription("Text to search for in configuration key names and descriptions") String query,
            @JsonRpcDescription("Optional extension prefix to filter by, e.g. 'datasource', 'oidc', 'hibernate-orm'") String extension,
            @JsonRpcDescription("Maximum number of results to return (default 50)") Integer limit) {

        int maxResults = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        String queryLower = query != null ? query.toLowerCase(Locale.ROOT) : null;
        List<String> extensionPrefixes = resolveExtensionPrefixes(extension);

        List<ConfigDescription> matches = new ArrayList<>();
        for (ConfigDescription config : configDescriptionBean.getAllConfig()) {
            if (extensionPrefixes != null && !matchesAnyPrefix(config.getName(), extensionPrefixes)) {
                continue;
            }
            if (queryLower != null) {
                String nameLower = config.getName().toLowerCase(Locale.ROOT);
                String descLower = config.getDescription() != null ? config.getDescription().toLowerCase(Locale.ROOT) : "";
                if (!nameLower.contains(queryLower) && !descLower.contains(queryLower)) {
                    continue;
                }
            }
            matches.add(config);
            if (matches.size() >= maxResults) {
                break;
            }
        }

        return toJsonArray(matches);
    }

    @JsonRpcDescription("Get all configuration keys for a specific Quarkus extension. Returns configuration properties with their full metadata including description, type, default value, current value, phase and allowed values.")
    @DevMCPEnableByDefault
    public JsonArray getExtensionConfig(
            @JsonRpcDescription("The extension prefix, e.g. 'datasource', 'oidc', 'rest-client', 'hibernate-orm'") String extension,
            @JsonRpcDescription("Maximum number of results to return (default 50)") Integer limit) {

        int maxResults = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        List<String> extensionPrefixes = resolveExtensionPrefixes(extension);
        if (extensionPrefixes == null) {
            return new JsonArray();
        }

        List<ConfigDescription> matches = new ArrayList<>();
        for (ConfigDescription config : configDescriptionBean.getAllConfig()) {
            if (matchesAnyPrefix(config.getName(), extensionPrefixes)) {
                matches.add(config);
                if (matches.size() >= maxResults) {
                    break;
                }
            }
        }

        return toJsonArray(matches);
    }

    @JsonRpcDescription("Get the project properties for the Quarkus application")
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

    /**
     * Resolves extension prefixes by first looking up the extension's configFilter metadata,
     * then falling back to a normalized prefix based on the extension name.
     */
    private List<String> resolveExtensionPrefixes(String extension) {
        if (extension == null || extension.isBlank()) {
            return null;
        }
        String trimmed = extension.trim();

        // Try metadata lookup first
        Map<String, List<String>> configFilters = configDescriptionBean.getExtensionConfigFilters();
        if (configFilters != null && !configFilters.isEmpty()) {
            List<String> prefixes = configFilters.get(trimmed.toLowerCase(Locale.ROOT));
            if (prefixes != null) {
                return prefixes;
            }
        }

        // Fall back to normalized prefix
        String prefix;
        if (trimmed.startsWith("quarkus.")) {
            prefix = trimmed.endsWith(".") ? trimmed : trimmed + ".";
        } else {
            prefix = "quarkus." + trimmed + ".";
        }
        return List.of(prefix);
    }

    private static boolean matchesAnyPrefix(String name, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static JsonArray toJsonArray(List<ConfigDescription> configs) {
        JsonArray result = new JsonArray();
        for (ConfigDescription config : configs) {
            JsonObject obj = new JsonObject();
            obj.put("name", config.getName());
            obj.put("description", config.getDescription());
            obj.put("defaultValue", config.getDefaultValue());
            if (config.getConfigValue() != null && config.getConfigValue().getValue() != null) {
                obj.put("currentValue", config.getConfigValue().getValue());
            }
            obj.put("type", config.getTypeName());
            obj.put("phase", config.getConfigPhase());
            if (config.getAllowedValues() != null && !config.getAllowedValues().isEmpty()) {
                obj.put("allowedValues", new JsonArray(config.getAllowedValues()));
            }
            result.add(obj);
        }
        return result;
    }
}
