package io.quarkus.devui.spi.page;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;

/**
 * Any of card, menu or footer pages
 */
public abstract class AbstractPageBuildItem extends AbstractDevUIBuildItem {

    protected final Map<String, Object> buildTimeData;

    public AbstractPageBuildItem(String extensionName) {
        super(extensionName);
        this.buildTimeData = new HashMap<>();
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
