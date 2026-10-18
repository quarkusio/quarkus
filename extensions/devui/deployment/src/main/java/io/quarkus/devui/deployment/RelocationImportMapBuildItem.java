package io.quarkus.devui.deployment;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Used internally to relocate namespaces for backward compatibility
 */
public final class RelocationImportMapBuildItem extends SimpleBuildItem {

    private final Map<String, String> relocations = new HashMap<>();

    public RelocationImportMapBuildItem() {

    }

    public void add(String from, String to) {
        this.relocations.put(from, to);
    }

    public Map<String, String> getRelocationMap() {
        return relocations;
    }
}
