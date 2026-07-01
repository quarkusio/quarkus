package io.quarkus.devmcp.runtime;

import java.util.HashMap;
import java.util.Map;

public class McpBuildTimeData {
    private final Map<String, String> urlAndPath = new HashMap<>();
    private final Map<String, String> descriptions = new HashMap<>();
    private final Map<String, String> mcpDefaultEnabled = new HashMap<>();
    private final Map<String, String> contentTypes = new HashMap<>();

    public void addData(Map<String, String> urlAndPath,
            Map<String, String> descriptions,
            Map<String, String> mcpDefaultEnabled,
            Map<String, String> contentTypes) {
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
}
