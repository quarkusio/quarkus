package io.quarkus.devui.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds information of Build Time static data
 */
public class DevUIBuildTimeStaticService {
    private final Map<String, String> urlAndPath = new HashMap<>();
    private final Map<String, String> descriptions = new HashMap<>();
    private String basePath; // Like /q/dev-ui

    public void addData(String basePath, Map<String, String> urlAndPath, Map<String, String> descriptions) {
        this.basePath = basePath;
        this.urlAndPath.putAll(urlAndPath);
        this.descriptions.putAll(descriptions);
    }

    public Map<String, String> getUrlAndPath() {
        return urlAndPath;
    }

    public Map<String, String> getDescriptions() {
        return descriptions;
    }

    public String getBasePath() {
        return basePath;
    }
}