package io.quarkus.devui.deployment;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeData;

/**
 * Write javascript file containing const vars with build time data
 */
public final class BuildTimeConstBuildItem extends AbstractDevUIBuildItem {

    private final Map<String, BuildTimeData> buildTimeData;

    public BuildTimeConstBuildItem() {
        this(new HashMap<>());
    }

    public BuildTimeConstBuildItem(Map<String, BuildTimeData> buildTimeData) {
        super();
        this.buildTimeData = buildTimeData;
    }

    public BuildTimeConstBuildItem(String customIdentifier) {
        this(customIdentifier, new HashMap<>());
    }

    public BuildTimeConstBuildItem(String customIdentifier, Map<String, BuildTimeData> buildTimeData) {
        super(customIdentifier);
        this.buildTimeData = buildTimeData;
    }

    public void addBuildTimeData(String fieldName, Object fieldData) {
        this.addBuildTimeData(fieldName, fieldData, null, false);
    }

    public void addBuildTimeData(String fieldName, Object fieldData, String description, boolean mcpEnabledAsDefault) {
        this.buildTimeData.put(fieldName, new BuildTimeData(fieldData, description, mcpEnabledAsDefault));
    }

    public void addAllBuildTimeData(Map<String, BuildTimeData> buildTimeData) {
        this.buildTimeData.putAll(buildTimeData);
    }

    public Map<String, BuildTimeData> getBuildTimeData() {
        return this.buildTimeData;
    }

    public boolean hasBuildTimeData() {
        return this.buildTimeData != null && !this.buildTimeData.isEmpty();
    }
}
