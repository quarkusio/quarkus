package io.quarkus.devui.deployment;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used internally to define some of our own imports
 */
public final class InternalImportMapBuildItem extends MultiBuildItem {

    private final Map<String, String> importMap = new HashMap<>();

    public InternalImportMapBuildItem() {

    }

    public void add(Map<String, String> importMap) {
        this.importMap.putAll(importMap);
    }

    public void add(String key, String path) {
        this.importMap.put(key, path);
    }

    public Map<String, String> getImportMap() {
        return importMap;
    }
}
