package io.quarkus.devui.deployment;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;

/**
 * Write javascript file containing const vars with build time data
 */
public final class BuildTimeConstBuildItem extends AbstractDevUIBuildItem {

    private final Map<String, Object> buildTimeData;

    public BuildTimeConstBuildItem(String extensionName) {
        this(extensionName, new HashMap<>());
    }

    public BuildTimeConstBuildItem(String extensionName, Map<String, Object> buildTimeData) {
        super(extensionName);
        this.buildTimeData = buildTimeData;
    }

    public void addBuildTimeData(String fieldName, Object fieldData) {
        this.buildTimeData.put(fieldName, fieldData);
    }

    public Map<String, Object> getBuildTimeData() {
        return this.buildTimeData;
    }

    public boolean hasBuildTimeData() {
        return this.buildTimeData != null && !this.buildTimeData.isEmpty();
    }
}
