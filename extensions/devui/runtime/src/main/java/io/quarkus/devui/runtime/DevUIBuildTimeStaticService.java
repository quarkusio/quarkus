package io.quarkus.devui.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds information of Build Time static data
 */
public class DevUIBuildTimeStaticService {
    private final Map<String, String> urlAndPath = new HashMap<>();
    private final Map<String, String> descriptions = new HashMap<>();
    private final Map<String, String> mcpDefaultEnabled = new HashMap<>();
    private final Map<String, String> contentTypes = new HashMap<>();

    private String basePath; // Like /q/dev-ui

    public void addData(String basePath,
            Map<String, String> urlAndPath,
            Map<String, String> descriptions,
            Map<String, String> mcpDefaultEnabled,
            Map<String, String> contentTypes) {
        this.basePath = basePath;
        this.urlAndPath.putAll(urlAndPath);
        this.descriptions.putAll(descriptions);
        this.mcpDefaultEnabled.putAll(mcpDefaultEnabled);
        this.contentTypes.putAll(contentTypes);
    }

    public Map<String, String> getUrlAndPath() {
        return urlAndPath;
    }

    public Map<String, String> getDescriptions() {
        return descriptions;
    }

    public Map<String, String> getMcpDefaultEnabled() {
        return mcpDefaultEnabled;
    }

    public Map<String, String> getContentTypes() {
        return contentTypes;
    }

    public String getBasePath() {
        return basePath;
    }
}