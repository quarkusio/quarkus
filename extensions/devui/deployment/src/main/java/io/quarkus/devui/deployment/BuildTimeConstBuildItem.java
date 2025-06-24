package io.quarkus.devui.deployment;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;

/**
 * Write javascript file containing const vars with build time data
 */
public final class BuildTimeConstBuildItem extends AbstractDevUIBuildItem {

    private final Map<String, Object> buildTimeData;

    public BuildTimeConstBuildItem() {
        this(new HashMap<>());
    }

    public BuildTimeConstBuildItem(Map<String, Object> buildTimeData) {
        super();
        this.buildTimeData = buildTimeData;
    }

    public BuildTimeConstBuildItem(String customIdentifier) {
        this(customIdentifier, new HashMap<>());
    }

    public BuildTimeConstBuildItem(String customIdentifier, Map<String, Object> buildTimeData) {
        super(customIdentifier);
        this.buildTimeData = buildTimeData;
    }

    public void addBuildTimeData(String fieldName, Object fieldData) {
        this.buildTimeData.put(fieldName, fieldData);
    }

    public void addAllBuildTimeData(Map<String, Object> buildTimeData) {
        this.buildTimeData.putAll(buildTimeData);
    }

    public Map<String, Object> getBuildTimeData() {
        return this.buildTimeData;
    }

    public boolean hasBuildTimeData() {
        return this.buildTimeData != null && !this.buildTimeData.isEmpty();
    }
}
